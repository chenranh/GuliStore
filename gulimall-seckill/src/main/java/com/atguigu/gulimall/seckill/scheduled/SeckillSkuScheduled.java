package com.atguigu.gulimall.seckill.scheduled;

import com.atguigu.gulimall.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 秒杀商品的定时上架
 * 每天晚上3点 上架最近三天需要秒杀的商品
 * 当天00:00:00-23:59:59
 * 明天00:00:00-23:59:59
 * 后天00:00:00-23:59:59
 *
 * @Author yuke
 * @Date: 2020-10-16 14:47
 */


@Slf4j
@Service
public class SeckillSkuScheduled {

    @Autowired
    SeckillService seckillService;

    @Autowired
    RedissonClient redissonClient;

    private final String UPLOAD_LOCK = "seckill:upload:lock";

    // TODO: 2020-10-16 上架幂等性 上架了就不能再上架了
    @Scheduled(cron = "* * * * * ?")
    public void uploadSeckillSkuLatest3Days() {
        System.out.println("上架了商品");

        //1.重复上架无需处理

        //分布式锁  解决分布式服务器共同上架的问题
        //锁的业务执行完成，状态已经更新完成，释放锁以后其它人获取到的是最新的状态
        RLock lock = redissonClient.getLock(UPLOAD_LOCK);

        lock.lock(10, TimeUnit.SECONDS);
        try {
            seckillService.uploadSeckillSkuLatest3Day();
        } finally {
            lock.unlock();
        }

    }

}
