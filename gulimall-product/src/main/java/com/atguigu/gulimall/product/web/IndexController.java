package com.atguigu.gulimall.product.web;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.forwebvo.Catelog2VO;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @title: IndexController
 * @Author yuke
 * @Date: 2020-09-07 14:24
 */
@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    RedisTemplate redisTemplate;

    @GetMapping({"/", "index.html"})
    public String indexPage(Model model) {
        // 查出所有一级数据展示在页面
        List<CategoryEntity> categoryEntities = categoryService.getLevel1Category();

        model.addAttribute("categories", categoryEntities);
        //视图解析器进行拼串
        //classpath:/templates/+返回值+ .html
        return "index";
    }

    //index/catalog.json
    //获取三级分类数据
    @ResponseBody
    @GetMapping("index/catelog.json")
    public Map<String, List<Catelog2VO>> getCataLogJson() {

        Map<String, List<Catelog2VO>> cataLogJson = categoryService.getCataLogJson();

        return cataLogJson;
    }

    //测试redisson分布式锁
    @ResponseBody
    @GetMapping("/hello")
    public String testRedisson() {
        //获取一把锁，只要锁名字一样，就是同一把锁
        RLock lock = redissonClient.getLock("mylock");
        //加锁
        //  lock.lock();//阻塞式等待。默认锁过期时间30秒

        //1.如果传递了锁的超时时间，就发送给redis执行脚本进行占锁，默认超时就是我们指定的时间
        //2.未指定锁的超时时间，就使用30*1000 lockwatchdogtimeout看门狗的默认时间，
        //2.1 只要占锁成功就会启动一个定时任务，重新给锁设置一个过期时间。新的过期时间就是看门狗的默认时间
        //2.2 业务在执行 看门狗会一直续期 lockwatchdogtimeout/3 每隔10秒再次续期成满时间30秒
        lock.lock(10, TimeUnit.SECONDS);//10秒后自动解锁，自动解锁时间一定要大于业务的执行时间，锁过期后不会自动续期
        //1.锁的自动续期  如果业务超长，运行期间自动给锁续到新的30秒，不用担心业务时间长，锁自动过期被删掉
        //2.加锁的业务只要运行完成，就不会给当前锁续期。即使不手动解锁。锁默认在30s以后自动删除

        //最佳实战
        //lock.lock(10, TimeUnit.SECONDS);省掉了整个续期操作，手动解锁
        try {
            System.out.println("加锁成功，执行业务" + Thread.currentThread().getId());
            Thread.sleep(30000);
        } catch (Exception e) {

        } finally {
            //解锁  假设解锁代码没有运行 redisson会不会出现死锁 不会
            System.out.println("释放锁" + Thread.currentThread().getId());
            //即使宕机没有解锁最后也会释放锁 Redisson中会为每个锁加上“leaseTime”，默认是30秒
            lock.unlock();
        }
        return "hello";
    }


    //测试redisson读写锁
    //写锁还没完成 读就读不出来，写锁与写锁之间不冲突
    //保证一定能读到最新的数据，修改期间是一个排他锁。读锁是一个共享锁
    @GetMapping("/write")
    @ResponseBody
    public String write() {
        RReadWriteLock lock = redissonClient.getReadWriteLock("rw-lock");
        String s = "";
        RLock rLock = lock.writeLock();
        try {
            //改数据加写锁，读数据加读锁
            rLock.lock();
            s = UUID.randomUUID().toString();
            Thread.sleep(3000);
            redisTemplate.opsForValue().set("writeVaule", s);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            //业务代码结束释放锁
            rLock.unlock();
        }
        return s;
    }

    //读锁
    @GetMapping("/read")
    @ResponseBody
    public String read() {

        RReadWriteLock lock = redissonClient.getReadWriteLock("rw-lock");
        String vaule = "";
        //加读锁
        RLock rLock = lock.readLock();
        rLock.lock();

        try {
            vaule = (String) redisTemplate.opsForValue().get("writeVaule");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //释放锁
            rLock.unlock();
        }

        return vaule;
    }

    // juc下的读写锁 用法的redisson一样
//        ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
//        ReentrantReadWriteLock.WriteLock writeLock = readWriteLock.writeLock();
//        writeLock.lock();

    /**
     * 信号量  可以用来做分布式限流
     * 车库停车
     * 3车位 来一辆车占用一个车位 想要停车需要看车位够不够
     *
     * @return
     */
    @GetMapping("/park")
    @ResponseBody
    public String park() throws InterruptedException {
        RSemaphore park = redissonClient.getSemaphore("park");
        // 同时最多允许3个线程获取锁
        park.trySetPermits(3);
        park.acquire();//获取一个信号量 获取一个值 占一个车位  阻塞式获取 一定获取才停止加载
        boolean b = park.tryAcquire(); //尝试获取
        if (b) {
            //执行业务
        } else {
            System.out.println("当前人流量大 稍等一会再获取");
        }
        return "ok";
    }

    //减为0后，park.acquire（）才能获取到
    @GetMapping("/go")
    @ResponseBody
    public String go() throws InterruptedException {
        RSemaphore park = redissonClient.getSemaphore("park");
        park.release();//释放一个车位
        return "ok";
    }


    /**
     * 闭锁
     * 放假锁门
     * 5个班全部走完，我们可以锁大门
     */
    @GetMapping("/lockDoor")
    @ResponseBody
    public String lockDoor() throws InterruptedException {
        RCountDownLatch door = redissonClient.getCountDownLatch("door");
        door.trySetCount(5);//5个班的计数
        door.await();//等待闭锁都完成
        return "放假了----";
    }

    @GetMapping("/gogogo/{id}")
    @ResponseBody
    public String gogogo(@PathVariable("id") Long id) {
        RCountDownLatch door = redissonClient.getCountDownLatch("door");
        door.countDown();//计数减一
        return id + "班的人都走了---";
    }

}
