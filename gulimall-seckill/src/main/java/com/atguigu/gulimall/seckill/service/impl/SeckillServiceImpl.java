package com.atguigu.gulimall.seckill.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
//import com.alibaba.csp.sentinel.Entry;
//import com.alibaba.csp.sentinel.SphU;
//import com.alibaba.csp.sentinel.annotation.SentinelResource;
//import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.mq.SecKillOrderTo;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRsepVo;
import com.atguigu.gulimall.seckill.feign.CouponFeignService;
import com.atguigu.gulimall.seckill.feign.ProductFeignService;
import com.atguigu.gulimall.seckill.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.seckill.service.SeckillService;
import com.atguigu.gulimall.seckill.to.SeckillSkuRedisTo;
import com.atguigu.gulimall.seckill.vo.SeckillSessionsWithSkus;
import com.atguigu.gulimall.seckill.vo.SeckillSkuRelationEntity;
import com.atguigu.gulimall.seckill.vo.SkuInfoVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import jodd.time.TimeUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import sun.rmi.runtime.Log;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @title: SeckillServiceImpl
 * @Author yuke
 * @Date: 2020-10-16 14:56
 */
@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {

    @Resource
    CouponFeignService couponFeignService;

    @Resource
    ProductFeignService productFeignService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final String SESSION_CACHE_PREFIX = "seckill:sessions:";

    private final String SKUKILL_CACHE_PREFIX = "seckill:skus:";

    private final String SKUSTOCK_SEMAPHONE = "seckill:stock:"; // +商品随机码

    /**
     * 定时任务扫描秒杀活动，先保存到数据库和redis
     */
    @Override
    public void uploadSeckillSkuLatest3Day() {
        //1、 扫描需要参与秒杀的活动
        R session = couponFeignService.getLate3DaySession();
        if (session.getCode() == 0) {
            //上架商品
            List<SeckillSessionsWithSkus> sessionData = session.getData(new TypeReference<List<SeckillSessionsWithSkus>>() {
            });
            //缓存到redis
            //1、缓存活动信息
            saveSessionInfos(sessionData);
            //2、缓存活动的关联商品信息
            saveSessionSkuInfos(sessionData);
        }
    }

//    public List<SeckillSkuRedisTo> blockHandler(BlockException e) {
//        log.error("getCurrentSeckillSkusResource被限流了");
//        return null;
//    }
    /**
     * 返回当前时间参与秒杀的商品
     * 前端首页页面显示
     * blockHandler 表示被限流了调用哪个方法，函数会在原方法被限流降级系统保护的时候调用
     * fallback函数会针对所有类型的异常
     * @return
     */
//    @SentinelResource(value = "getCurrentSeckillSkusResource", blockHandler = "blockHander")
    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {
        //1.确定当前时间属于哪个秒杀场次
        long time = new Date().getTime();

        //try catch 用于sentinel自定义保护资源测试
//        try (Entry entry = SphU.entry("seckillSkus")) {
            //在redis中查到  seckill:sessions:开头的所有的key
            Set<String> keys = stringRedisTemplate.keys(SESSION_CACHE_PREFIX + "*");
            for (String key : keys) {
                String replace = key.replace(SESSION_CACHE_PREFIX, "");
                String[] s = replace.split("_");
                long start = Long.parseLong(s[0]);
                long end = Long.parseLong(s[1]);
                if (time >= start && time <= end) {
                    //2.获取这个秒杀场次需要的所有商品信息  拿到list 4_2这样的数据
                    List<String> range = stringRedisTemplate.opsForList().range(key, -100, 100);
                    BoundHashOperations<String, String, String> hashOps = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
                    List<String> list = hashOps.multiGet(range);
                    if (CollectionUtil.isNotEmpty(list)) {
                        List<SeckillSkuRedisTo> collect = list.stream().map(item -> {
                            SeckillSkuRedisTo redis = JSON.parseObject((String) item, SeckillSkuRedisTo.class);
                            //redis.setRandomCode(null); 当前秒杀开始需要的随机码
                            return redis;
                        }).collect(Collectors.toList());
                        return collect;
                    }
                    break;
                }

            }
//        } catch (BlockException e) {
//            log.error("资源被限流", e.getMessage());
//            e.printStackTrace();
//        }


        return null;
    }

    /**
     * 获取某个商品的秒杀信息
     * 单个sku商品显示秒杀信息
     *
     * @param skuId
     * @return
     */
    @Override
    public SeckillSkuRedisTo getSkuSeckillInfo(Long skuId) {
        //1.找到所有需要参与秒杀的商品的key
        BoundHashOperations<String, String, String> hashOps = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        Set<String> keys = hashOps.keys();
        if (CollectionUtil.isNotEmpty(keys)) {
            //d表示匹配到的是一个数字
            String regx = "\\d_" + skuId;
            for (String key : keys) {
                //key 6_4
                if (Pattern.matches(regx, key)) {
                    String json = hashOps.get(key);
                    SeckillSkuRedisTo skuRedisTo = JSON.parseObject(json, SeckillSkuRedisTo.class);
                    //随机码处理 当前时间在秒杀时间范围内 把随机码传给前端页面
                    long current = new Date().getTime();
                    if (current >= skuRedisTo.getStartTime() && current <= skuRedisTo.getEndTime()) {

                    } else {
                        skuRedisTo.setRandomCode(null);
                    }
                    return skuRedisTo;
                }
            }
        }

        return null;
    }


    /**
     * 点击秒杀流程
     * todo 上架秒杀商品的时候每一个数据都有过期时间
     * todo 上架的时候应该把库存服务里的商品库存锁定住，秒杀结束后如果redis中还有剩下的再给库存加回去。这里简化了收货地址等信息
     *
     * @param killId
     * @param key
     * @param num
     * @return
     */
    @Override
    public String kill(String killId, String key, Integer num) {

        MemberRsepVo rsepVo = LoginUserInterceptor.threadLocal.get();

        //1.获取当前秒杀商品的详细信息
        BoundHashOperations<String, String, String> hashOps = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        String json = hashOps.get(killId);
        if (StringUtils.isEmpty(json)) {
            return null;
        } else {
            SeckillSkuRedisTo redis = JSON.parseObject(json, SeckillSkuRedisTo.class);
            //2.校验合法性
            Long startTime = redis.getStartTime();
            Long endTime = redis.getEndTime();
            long time = new Date().getTime();
            long ttl = endTime - startTime;
            //2.1 校验时间合法性  redis中应该给秒杀活动过期时间
            if (time >= startTime && time <= endTime) {
                //2.2检验随机码和商品id
                String randomCode = redis.getRandomCode();
                String skuId = redis.getPromotionSessionId() + "_" + redis.getSkuId();
                if (randomCode.equals(key) && killId.equals(skuId)) {
                    //2.3验证购物的数量是否合理
                    if (num <= redis.getSeckillLimit()) {
                        //2.4验证这个人是否已经购买过 幂等性；如果秒杀成功，就去占位。redis保存key为userId_SessionId_key
                        String redisKey = rsepVo.getId() + skuId;
                        //setIfAbsent() redis不存在的时候占位
                        //自动过期  活动时间结束 占位删除
                        Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(redisKey, num.toString(), ttl, TimeUnit.MILLISECONDS);
                        //占位成功说明这个人从来没有买过
                        if (aBoolean) {
                            //3.获取该商品的信号量
                            RSemaphore semaphore = redissonClient.getSemaphore(SKUSTOCK_SEMAPHONE + randomCode);
                            //快速尝试能否拿得到信号量
                            boolean acquire = semaphore.tryAcquire(num);

                            if (acquire) {
                                //秒杀成功
                                //快速下单 timeId作为订单号。发MQ消息
                                String timeId = IdWorker.getTimeId();
                                SecKillOrderTo orderTo = new SecKillOrderTo();
                                orderTo.setOrderSn(timeId);
                                orderTo.setMemberId(rsepVo.getId());
                                orderTo.setNum(num);
                                orderTo.setPromotionSessionId(redis.getPromotionSessionId());
                                orderTo.setSkuId(redis.getSkuId());
                                orderTo.setSeckillPrice(redis.getSeckillPrice());
                                rabbitTemplate.convertAndSend("order-event-exchange", "order.seckill.order", orderTo);
                                return timeId;
                            }
                            return null;

                        } else {
                            //说明已经买过
                            return null;
                        }
                    }
                }
            }
        }
        return null;
    }

    private void saveSessionInfos(List<SeckillSessionsWithSkus> sessions) {
        sessions.stream().forEach(session -> {
            Long startTime = session.getStartTime().getTime();
            Long endTime = session.getEndTime().getTime();
            String key = SESSION_CACHE_PREFIX + startTime + "_" + endTime;
            //缓存活动信息  如果key已经存在就不再保存了，解决活动重复保存的问题
            Boolean hasKey = stringRedisTemplate.hasKey(key);
            if (!hasKey) {
                List<String> collect = session.getRelationSkus().stream().map(item -> item.getPromotionSessionId() + "_" + item.getSkuId()).collect(Collectors.toList());
                stringRedisTemplate.opsForList().leftPushAll(key, collect);
            }

        });

    }

    private void saveSessionSkuInfos(List<SeckillSessionsWithSkus> sessions) {
        if (sessions != null) {
            sessions.stream().forEach(session -> {
                //准备hash操作
                BoundHashOperations<String, Object, Object> ops = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);

                session.getRelationSkus().stream().forEach(seckillSkuVo -> {


                    //如果redis中已经存在就不在存放，解决重复上架秒杀活动问题
                    if (!ops.hasKey(seckillSkuVo.getPromotionId() + "_" + seckillSkuVo.getSkuId())) {
                        //缓存商品
                        SeckillSkuRedisTo redisTo = new SeckillSkuRedisTo();

                        //1.sku的基本数据
                        R skuInfo = productFeignService.skuInfo(seckillSkuVo.getSkuId());
                        if (skuInfo.getCode() == 0) {
                            SkuInfoVo info = skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                            });
                            redisTo.setSkuInfoVo(info);
                        }
                        //2.sku的秒杀信息
                        BeanUtil.copyProperties(seckillSkuVo, redisTo);

                        //3.设置上当前商品的秒杀时间信息
                        redisTo.setStartTime(session.getStartTime().getTime());
                        redisTo.setEndTime(session.getEndTime().getTime());

                        //4.商品秒杀的随机码  公平秒杀，减库存时也需要这个随机码
                        String token = UUID.randomUUID().toString().replace("-", "");

                        redisTo.setRandomCode(token);
                        String jsonString = JSON.toJSONString(redisTo);
                        ops.put(seckillSkuVo.getPromotionId() + "_" + seckillSkuVo.getSkuId(), jsonString);

                        //5.使用库存作为引入redisson的信号量  作用是限流！！！！！！！！！！
                        RSemaphore semaphore = redissonClient.getSemaphore(SKUSTOCK_SEMAPHONE + token);
                        //商品可以秒杀的数量作为信号量
                        semaphore.trySetPermits(seckillSkuVo.getSeckillCount());
                    }

                });
            });
        }
    }

}


