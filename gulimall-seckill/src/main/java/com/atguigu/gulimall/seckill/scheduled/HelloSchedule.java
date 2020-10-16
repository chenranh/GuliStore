package com.atguigu.gulimall.seckill.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


/**
 * 定时任务
 *      1. @EnableScheduling 开启定时任务
 *      2. @Scheduled 开启一个定时任务
 *      3. spring自动配置类 TaskSchedulingProperties
 *
 * 异步任务
 *     1. @EnableAsync开启异步任务功能
 *     2. @Async 给希望异步执行的方法上标注
 *     3. spring自动配置类 TaskExecutionAutoConfiguration  spring.task.execution
 *
 */

@Slf4j
@Component
//@EnableScheduling
//@EnableAsync
public class HelloSchedule {

    /**
     * 1.spring中6位组成，不允许第七位 年
     * 2.在周几的位置 1-7代表周一到周日
     * 3.定时任务不应该阻塞。默认是阻塞的
     *      1）、可以让业务运行已异步的方式，自己提交到线程池
     *       CompletableFuture.runAsync(()->{
     *               ***service.hello()
     *         },executor);
     *      2)、定时任务的线程池；通过设置TaskSchedulingProperties spring.task.scheduling改变线程池里的线程个数
     *             spring.task.scheduling.pool.size=5 (不好使）
     *
     *      3)、让定时任务异步执行
     *          异步任务；
     *
     *     解决：异步任务加定时任务来完成定时任务不阻塞功能
     *
     */


    @Async
    @Scheduled(cron = "* * * * * ? ")//每秒都会打印  秒分时日月周
    public void hello(){
        log.info("hello...");
//        ***service.hello() 默认是需要hello（）方法走完才能执行接下来的代码

    }


}
