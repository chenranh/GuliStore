package com.atguigu.gulimall.seckill.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.seckill.feign.CouponFeignService;
import com.atguigu.gulimall.seckill.feign.ProductFeignService;
import com.atguigu.gulimall.seckill.service.SeckillService;
import com.atguigu.gulimall.seckill.to.SeckillSkuRedisTo;
import com.atguigu.gulimall.seckill.vo.SeckillSessionsWithSkus;
import com.atguigu.gulimall.seckill.vo.SeckillSkuRelationEntity;
import com.atguigu.gulimall.seckill.vo.SkuInfoVo;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @title: SeckillServiceImpl
 * @Author yuke
 * @Date: 2020-10-16 14:56
 */
@Service
public class SeckillServiceImpl implements SeckillService {

    @Resource
    CouponFeignService couponFeignService;

    @Resource
    ProductFeignService productFeignService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final String SESSION_CACHE_PREFIX = "seckill:sessions:";

    private final String SKUKILL_CACHE_PREFIX = "seckill:skus:";

    private final String SKUSTOCK_SEMAPHONE = "seckill:stock:"; // +商品随机码

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

    private void saveSessionInfos(List<SeckillSessionsWithSkus> sessions) {
        sessions.stream().forEach(session -> {
            Long startTime = session.getStartTime().getTime();
            Long endTime = session.getEndTime().getTime();
            String key = SESSION_CACHE_PREFIX + startTime + "_" + endTime;
            //缓存活动信息  如果key已经存在就不再保存了，解决活动重复保存的问题
            Boolean hasKey = stringRedisTemplate.hasKey(key);
            if (!hasKey) {
                List<String> collect = session.getRelationSkus().stream().map(item -> item.getPromotionId()+"_"+item.getSkuId()).collect(Collectors.toList());
                stringRedisTemplate.opsForList().leftPushAll(key, collect);
            }

        });

    }

    private void saveSessionSkuInfos(List<SeckillSessionsWithSkus> sessions) {
        sessions.stream().forEach(session -> {
            //准备hash操作
            BoundHashOperations<String, Object, Object> ops = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);

            session.getRelationSkus().stream().forEach(seckillSkuVo -> {


                //如果redis中已经存在就不在存放，解决重复上架秒杀活动问题
                if (!ops.hasKey(seckillSkuVo.getPromotionId()+"_"+seckillSkuVo.getSkuId())) {
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
                    ops.put(seckillSkuVo.getPromotionId()+"_"+seckillSkuVo.getSkuId(), jsonString);

                    //5.使用库存作为引入redisson的信号量  作用是限流！！！！！！！！！！
                    RSemaphore semaphore = redissonClient.getSemaphore(SKUSTOCK_SEMAPHONE + token);
                    //商品可以秒杀的数量作为信号量
                    semaphore.trySetPermits(seckillSkuVo.getSeckillCount());
                }

            });
        });
    }


}
