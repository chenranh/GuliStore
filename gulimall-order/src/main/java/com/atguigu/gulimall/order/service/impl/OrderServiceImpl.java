package com.atguigu.gulimall.order.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.enume.OrderStatusEnum;
import com.atguigu.common.to.mq.OrderTo;
import com.atguigu.common.to.mq.SecKillOrderTo;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRsepVo;
import com.atguigu.gulimall.order.constant.OrderConstant;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.atguigu.gulimall.order.entity.PaymentInfoEntity;
import com.atguigu.gulimall.order.exception.NoStockException;
import com.atguigu.gulimall.order.feign.CartFeignService;
import com.atguigu.gulimall.order.feign.MembenFeignService;
import com.atguigu.gulimall.order.feign.ProductFeignService;
import com.atguigu.gulimall.order.feign.WmsFeignService;
import com.atguigu.gulimall.order.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.order.service.OrderItemService;
import com.atguigu.gulimall.order.service.PaymentInfoService;
import com.atguigu.gulimall.order.to.OrderCreateTo;
import com.atguigu.gulimall.order.vo.*;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.order.dao.OrderDao;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.service.OrderService;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {


    private ThreadLocal<OrderSubmitVo> confirmVoThreadLocal = new ThreadLocal<>();

    @Autowired
    MembenFeignService membenFeignService;

    @Autowired
    CartFeignService cartFeignService;

    @Autowired
    ThreadPoolExecutor executor;

    @Autowired
    WmsFeignService wmsFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    private PaymentInfoService paymentInfoService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 确认订单
     *
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        //注意这里对 threadLocal的应用
        MemberRsepVo memberRsepVo = LoginUserInterceptor.threadLocal.get();
        //异步线程调用feign请求头丢失解决：从主线程里拿到数据 共享到副线程
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        CompletableFuture<Void> memberFuture = CompletableFuture.runAsync(() -> {
            //1.远程查询所有的地址列表
            //异步线程需要额外加上RequestContextHolder.setRequestAttributes()，共享主线程请求数据
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<MemberAddressVo> address = membenFeignService.getAddress(memberRsepVo.getId());
            confirmVo.setAddress(address);
        }, executor);


        //异步任务出现问题可以用exceptionally()或者whencomplete处理
        CompletableFuture<Void> cartFuture = CompletableFuture.runAsync(() -> {
            //2.远程查询购物车所有选中的购物项
            //fegin在远程调用之前要构造请求，调用很多的拦截器RequestInterceptor，需要自己添加拦截器。不然会丢失老请求里请求头内容，取不到当前登录用户
            //异步线程需要额外加上RequestContextHolder.setRequestAttributes()，共享主线程请求数据
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<OrderItemVo> items = cartFeignService.getCurrentUserCartItems();
            confirmVo.setItems(items);
            //2.1 判断有货无货（无货不应该不能添加吗？？） 不是登录状态下查询库存信息不需要共享数据不用RequestContextHolder.setRequestAttributes
        }, executor).thenRunAsync(() -> {
            List<OrderItemVo> items = confirmVo.getItems();
            List<Long> collect = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
            R skuHasStock = wmsFeignService.getSkuHasStock(collect);
            List<SkuStockVo> data = skuHasStock.getData(new TypeReference<List<SkuStockVo>>() {
            });
            if (!CollectionUtils.isEmpty(data)) {
                Map<Long, Boolean> map = data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                confirmVo.setStocks(map);
            }
        }, executor);


        //3.查询用户积分
        Integer integration = memberRsepVo.getIntegration();
        confirmVo.setIntegration(integration);
        //4.其他数据根据items自动计算 订单的总额

        //5.todo 防重令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        //redis存一份
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRsepVo.getId(), token, 30, TimeUnit.MINUTES);
        //页面存一份
        confirmVo.setOrderToken(token);


        CompletableFuture.allOf(cartFuture, memberFuture).get();

        return confirmVo;
    }

    /**
     * 提交订单
     *
     * @param submitVo
     * @return
     */
    //本地事务在分布式系统下只能控制住自己的回滚，控制不了其他服务的回滚
    //分布式事务：最大原因 网络问题和分布式机器
//    @GlobalTransactional seata的at模式（2pc的演化）不适应此高并发场景
    @Transactional(isolation = Isolation.REPEATABLE_READ)//设置隔离级别
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo submitVo) {
        confirmVoThreadLocal.set(submitVo);
        SubmitOrderResponseVo response = new SubmitOrderResponseVo();

        MemberRsepVo memberRsepVo = LoginUserInterceptor.threadLocal.get();
        // 0：正常
        response.setCode(0);

        //下单：去创建订单，验证令牌，验证价格，锁库存...
        //1.验证令牌【核心：令牌的对比和删除必须保证原子性】
        //0令牌删除失败-1令牌删除成功
        String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        String orderToken = submitVo.getOrderToken();
        // 原子验证令牌和删除令牌 返回值类型默认long类型,第三个参数是要对比的值
        Long result = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class),
                Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRsepVo.getId()), orderToken);
        if (result == 0L) {
            //令牌验证失败
            response.setCode(1);
            return response;
        } else {
            //令牌验证成功
            //createOrder()创建订单和订单项和金额对比等信息
            OrderCreateTo order = createOrder();
            //2.验证价格 减去优惠券这些
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = submitVo.getPayPrice();
            //金额对比
            if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {
                //todo 3.保存订单
                saveOrder(order);
                //4.库存锁定 只要有异常回滚订单数据
                //订单号，所有订单项（skuid，skuname，num）
                WareSkuLockVo lockVo = new WareSkuLockVo();
                lockVo.setOrderSn(order.getOrder().getOrderSn());
                List<OrderItemVo> collect = order.getOrderItems().stream().map(item -> {
                    OrderItemVo itemVo = new OrderItemVo();
                    itemVo.setSkuId(item.getSkuId());
                    itemVo.setCount(item.getSkuQuantity());
                    itemVo.setTitle(item.getSkuName());
                    return itemVo;
                }).collect(Collectors.toList());
                lockVo.setLocks(collect);
                // TODO: 2020-10-04 4.远程锁库存 很重要
                //已经执行完成的远程服务不会回滚，库存成功了但是网络原因超时了，订单回滚，库存不会回滚
                //为了保证高并发  库存服务自己回滚。可以发消息给库存服务
                //库存服务本身页可以使用自动解锁模式。消息队列
                R r = wmsFeignService.orderLockStock(lockVo);
                if (r.getCode() == 0) {
                    //锁定成功
                    response.setOrderEntity(order.getOrder());
                    //todo 5.远程扣减积分 出异常
                    //出问题订单回滚，库存不回滚
                    //int i=10/0; 为了测试库存锁定后 后面的流程出现问题 库存服务使用rabbitmq解锁库存
                    //todo 6.订单创建成功发送消息给MQ 过期不支付就会取消订单
                    rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", order.getOrder(), new CorrelationData(UUID.randomUUID().toString()));
                    return response;
                } else {
                    // 锁定失败
                    String msg = (String) r.get("msg");
                    throw new NoStockException(msg);
                }
            } else {
                response.setCode(2);
                return response;
            }
        }
    }

    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        return this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
    }

    /**
     * 关闭订单
     * 先查询订单的当前状态有没有完成
     *
     * @param entity
     */
    @Override
    public void closeOrder(OrderEntity entity) {
        //查询当前这个订单的最新状态
        OrderEntity orderEntity = this.getById(entity.getId());
        if (orderEntity.getStatus() == OrderStatusEnum.CREATE_NEW.getCode()) {
            //关单
            OrderEntity update = new OrderEntity();
            update.setId(entity.getId());
            //关闭订单 改变订单的状态
            update.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(update);
            OrderTo orderTo = new OrderTo();
            BeanUtil.copyProperties(orderEntity, orderTo);
            //发给MQ一个消息 用于解锁库存
            try {
                //todo 保证消息一定会发送出去，每一个消息都可以做好日志记录，重写消息的id
                //消息丢失情景1：消息发送出去，由于网络原因没有抵达服务器
                //解决：数据库中有一张消息记录表mq_message，消息发送给mq在消息确认中保存消息的内容和消息的状态信息（0新建 1错误抵达 ）,在MyRabbitConfig2有实现
                //todo 定期扫描数据库将失败的消息再发送一次
                rabbitTemplate.convertAndSend("order-event-exchange", "order.release.other", orderTo, new CorrelationData(UUID.randomUUID().toString()));
            } catch (Exception e) {
                //todo 将没发送成功的消息进行重试发送
                e.printStackTrace();
            }
        }
    }

    /**
     * 支付宝支付
     *
     * @param orderSn
     * @return
     */
    @Override
    public PayVo getOrderPay(String orderSn) {
        PayVo payVo = new PayVo();
        OrderEntity order = this.getOrderByOrderSn(orderSn);
        //BigDecimal.ROUND_UP 意思是66.00001保留两位成66.01
        BigDecimal bigDecimal = order.getPayAmount().setScale(2, BigDecimal.ROUND_UP);
        payVo.setTotal_amount(bigDecimal.toString());
        payVo.setOut_trade_no(order.getOrderSn());
        List<OrderItemEntity> order_sn = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
        OrderItemEntity entity = order_sn.get(0);
        payVo.setSubject(entity.getSkuName());
        payVo.setBody(entity.getSkuAttrsVals());

        return payVo;
    }

    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {
        MemberRsepVo rsepVo = LoginUserInterceptor.threadLocal.get();
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                // 查询这个用户的最新订单 [降序排序]
                new QueryWrapper<OrderEntity>().eq("member_id", rsepVo.getId()).orderByDesc("id")
        );
        List<OrderEntity> order_sn = page.getRecords().stream().map(order -> {
            // 查询这个订单关联的所有订单项
            List<OrderItemEntity> orderSn = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", order.getOrderSn()));
            order.setItemEntities(orderSn);
            return order;
        }).collect(Collectors.toList());
        page.setRecords(order_sn);
        return new PageUtils(page);
    }

    @Override
    public String handlePayResult(PayAsyncVo vo) {
        // 1.保存交易流水
        PaymentInfoEntity infoEntity = new PaymentInfoEntity();
        infoEntity.setAlipayTradeNo(vo.getTrade_no());
        infoEntity.setOrderSn(vo.getOut_trade_no());
        //		TRADE_SUCCESS
        infoEntity.setPaymentStatus(vo.getTrade_status());
        infoEntity.setCallbackTime(vo.getNotify_time());
        infoEntity.setSubject(vo.getSubject());
        infoEntity.setTotalAmount(new BigDecimal(vo.getTotal_amount()));
        infoEntity.setCreateTime(vo.getGmt_create());
        paymentInfoService.save(infoEntity);

        // 2.修改订单状态信息
        if (vo.getTrade_status().equals("TRADE_SUCCESS") || vo.getTrade_status().equals("TRADE_FINISHED")) {
            // 支付成功
            String orderSn = vo.getOut_trade_no();
            this.baseMapper.updateOrderStatus(orderSn, OrderStatusEnum.PAYED.getCode());
        }
        return "success";
    }

    /**
     * 创建秒杀订单
     *
     * @param secKillOrder
     */
    @Override
    public void createSeckillOrder(SecKillOrderTo secKillOrder) {
        // TODO: 2020-10-20 保存订单信息 还有很多字段没设置
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(secKillOrder.getOrderSn());
        orderEntity.setMemberId(secKillOrder.getMemberId());
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        BigDecimal price = secKillOrder.getSeckillPrice().multiply(new BigDecimal("" + secKillOrder.getNum()));
        orderEntity.setPayAmount(price);
        this.save(orderEntity);
        // TODO: 2020-10-20 保存订单项信息
        OrderItemEntity itemEntity = new OrderItemEntity();
        itemEntity.setOrderSn(secKillOrder.getOrderSn());
        itemEntity.setRealAmount(price);
        itemEntity.setOrderId(orderEntity.getId());
        itemEntity.setSkuQuantity(secKillOrder.getNum());
        R info = productFeignService.getSkuInfoBySkuId(secKillOrder.getSkuId());
        SpuInfoVo spuInfo = info.getData(new TypeReference<SpuInfoVo>() {
        });
        itemEntity.setSpuId(spuInfo.getId());
        itemEntity.setSpuBrand(spuInfo.getBrandId().toString());
        itemEntity.setSpuName(spuInfo.getSpuName());
        itemEntity.setCategoryId(spuInfo.getCatelogId());
        itemEntity.setGiftGrowth(secKillOrder.getSeckillPrice().multiply(new BigDecimal(secKillOrder.getNum())).intValue());
        itemEntity.setGiftIntegration(secKillOrder.getSeckillPrice().multiply(new BigDecimal(secKillOrder.getNum())).intValue());
        itemEntity.setPromotionAmount(new BigDecimal("0.0"));
        itemEntity.setCouponAmount(new BigDecimal("0.0"));
        itemEntity.setIntegrationAmount(new BigDecimal("0.0"));
        orderItemService.save(itemEntity);

    }

    /**
     * 保存订单所有数据
     */
    private void saveOrder(OrderCreateTo order) {
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        this.save(orderEntity);

        List<OrderItemEntity> orderItems = order.getOrderItems();
        orderItems = orderItems.stream().map(item -> {
            item.setOrderId(orderEntity.getId());
            item.setSpuName(item.getSpuName());
            item.setOrderSn(order.getOrder().getOrderSn());
            return item;
        }).collect(Collectors.toList());
        orderItemService.saveBatch(orderItems);
    }


    /**
     * 创建订单
     */
    private OrderCreateTo createOrder() {
        OrderCreateTo createTo = new OrderCreateTo();
        //1.生成订单号
        String orderSn = IdWorker.getTimeId();
        //创建订单号
        OrderEntity orderEntity = buildOrder(orderSn);
        //2.获取到所有的订单项
        List<OrderItemEntity> itemEntities = buildOrderItems(orderSn);
        //3.计算价格相关
        computerPrice(orderEntity, itemEntities);
        createTo.setOrder(orderEntity);
        createTo.setOrderItems(itemEntities);

        return createTo;
    }

    /**
     * 后端验证价格 防止前端篡改价格
     *
     * @param orderEntity
     * @param items
     */
    private void computerPrice(OrderEntity orderEntity, List<OrderItemEntity> items) {
        BigDecimal totalPrice = new BigDecimal("0.0");
        // 叠加每一个订单项的金额
        BigDecimal coupon = new BigDecimal("0.0");
        BigDecimal integration = new BigDecimal("0.0");
        BigDecimal promotion = new BigDecimal("0.0");
        BigDecimal gift = new BigDecimal("0.0");
        BigDecimal growth = new BigDecimal("0.0");
        for (OrderItemEntity item : items) {
            // 优惠券的金额
            coupon = coupon.add(item.getCouponAmount());
            // 积分优惠的金额
            integration = integration.add(item.getIntegrationAmount());
            // 打折的金额
            promotion = promotion.add(item.getPromotionAmount());
            BigDecimal realAmount = item.getRealAmount();
            totalPrice = totalPrice.add(realAmount);

            // 购物获取的积分、成长值
            gift.add(new BigDecimal(item.getGiftIntegration().toString()));
            growth.add(new BigDecimal(item.getGiftGrowth().toString()));
        }
        // 1.订单价格相关 总额、应付总额
        orderEntity.setTotalAmount(totalPrice);
        orderEntity.setPayAmount(totalPrice.add(orderEntity.getFreightAmount()));

        orderEntity.setPromotionAmount(promotion);
        orderEntity.setIntegrationAmount(integration);
        orderEntity.setCouponAmount(coupon);

        // 设置积分、成长值
        orderEntity.setIntegration(gift.intValue());
        orderEntity.setGrowth(growth.intValue());

        // 设置订单的删除状态
        orderEntity.setDeleteStatus(OrderStatusEnum.CREATE_NEW.getCode());
    }

    /**
     * 构建一个订单
     */
    private OrderEntity buildOrder(String orderSn) {
        OrderEntity entity = new OrderEntity();
        entity.setOrderSn(orderSn);
        entity.setCreateTime(new Date());
        entity.setCommentTime(new Date());
        entity.setReceiveTime(new Date());
        entity.setDeliveryTime(new Date());
        MemberRsepVo rsepVo = LoginUserInterceptor.threadLocal.get();
        entity.setMemberId(rsepVo.getId());
        entity.setMemberUsername(rsepVo.getUsername());
        entity.setBillReceiverEmail(rsepVo.getEmail());
        // 2. 获取收获地址信息
        OrderSubmitVo submitVo = confirmVoThreadLocal.get();
        R fare = wmsFeignService.getFare(submitVo.getAddrId());
        FareVo resp = fare.getData(new TypeReference<FareVo>() {
        });
        entity.setFreightAmount(resp.getFare());
        entity.setReceiverCity(resp.getMemberAddressVo().getCity());
        entity.setReceiverDetailAddress(resp.getMemberAddressVo().getDetailAddress());
        entity.setDeleteStatus(OrderStatusEnum.CREATE_NEW.getCode());
        entity.setReceiverPhone(resp.getMemberAddressVo().getPhone());
        entity.setReceiverName(resp.getMemberAddressVo().getName());
        entity.setReceiverPostCode(resp.getMemberAddressVo().getPostCode());
        entity.setReceiverProvince(resp.getMemberAddressVo().getProvince());
        entity.setReceiverRegion(resp.getMemberAddressVo().getRegion());
        // 设置订单状态
        entity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        entity.setAutoConfirmDay(7);
        return entity;
    }

    /**
     * 创建所有订单项数据
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        // 这里是最后一次来确认购物项的价格 这个远程方法还会查询一次数据库
        List<OrderItemVo> cartItems = cartFeignService.getCurrentUserCartItems();
        List<OrderItemEntity> itemEntities = null;
        if (cartItems != null && cartItems.size() > 0) {
            itemEntities = cartItems.stream().map(cartItem -> {
                OrderItemEntity itemEntity = buildOrderItem(cartItem);
                itemEntity.setOrderSn(orderSn);
                return itemEntity;
            }).collect(Collectors.toList());
        }
        return itemEntities;
    }

    /**
     * 构建某一个订单项
     */
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        OrderItemEntity itemEntity = new OrderItemEntity();
        // 1.订单信息： 订单号

        // 2.商品spu信息
        Long skuId = cartItem.getSkuId();
        R r = productFeignService.getSkuInfoBySkuId(skuId);
        SpuInfoVo spuInfo = r.getData(new TypeReference<SpuInfoVo>() {
        });
        itemEntity.setSpuId(spuInfo.getId());
        itemEntity.setSpuBrand(spuInfo.getBrandId().toString());
        itemEntity.setSpuName(spuInfo.getSpuName());
        itemEntity.setCategoryId(spuInfo.getCatelogId());
        // 3.商品的sku信息
        itemEntity.setSkuId(cartItem.getSkuId());
        itemEntity.setSkuName(cartItem.getTitle());
        itemEntity.setSkuPic(cartItem.getImage());
        itemEntity.setSkuPrice(cartItem.getPrice());
        // 把一个集合按照指定的字符串进行分割得到一个字符串
        String skuAttr = StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";");
        itemEntity.setSkuAttrsVals(skuAttr);
        itemEntity.setSkuQuantity(cartItem.getCount());
        // 4.积分信息 买的数量越多积分越多 成长值越多
        itemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount())).intValue());
        itemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount())).intValue());
        // 5.订单项的价格信息 优惠金额
        itemEntity.setPromotionAmount(new BigDecimal("0.0"));
        itemEntity.setCouponAmount(new BigDecimal("0.0"));
        itemEntity.setIntegrationAmount(new BigDecimal("0.0"));
        // 当前订单项的实际金额
        BigDecimal orign = itemEntity.getSkuPrice().multiply(new BigDecimal(itemEntity.getSkuQuantity().toString()));
        // 减去各种优惠的价格
        BigDecimal subtract = orign.subtract(itemEntity.getCouponAmount())
                .subtract(itemEntity.getPromotionAmount())
                .subtract(itemEntity.getIntegrationAmount());
        itemEntity.setRealAmount(subtract);
        return itemEntity;
    }

}
