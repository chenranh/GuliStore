package com.atguigu.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRsepVo;
import com.atguigu.gulimall.order.feign.CartFeignService;
import com.atguigu.gulimall.order.feign.MembenFeignService;
import com.atguigu.gulimall.order.feign.WmsFeignService;
import com.atguigu.gulimall.order.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.order.vo.MemberAddressVo;
import com.atguigu.gulimall.order.vo.OrderConfirmVo;
import com.atguigu.gulimall.order.vo.OrderItemVo;
import com.atguigu.gulimall.order.vo.SkuStockVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.order.dao.OrderDao;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.service.OrderService;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {


    @Autowired
    MembenFeignService membenFeignService;

    @Autowired
    CartFeignService cartFeignService;

    @Autowired
    ThreadPoolExecutor executor;

    @Autowired
    WmsFeignService wmsFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

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
        }, executor).thenRunAsync(()->{
            List<OrderItemVo> items = confirmVo.getItems();
            List<Long> collect = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
            R skuHasStock = wmsFeignService.getSkuHasStock(collect);
            List<SkuStockVo> data = skuHasStock.getData(new TypeReference<List<SkuStockVo>>() {});
            if (!CollectionUtils.isEmpty(data)){
                Map<Long, Boolean> map = data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                confirmVo.setStocks(map);
            }
        }, executor);


        //3.查询用户积分
        Integer integration = memberRsepVo.getIntegration();
        confirmVo.setIntegration(integration);
        //4.其他数据根据items自动计算 订单的总额

        //5.todo 防重令牌
        CompletableFuture.allOf(cartFuture,memberFuture).get();

        return confirmVo;
    }

}
