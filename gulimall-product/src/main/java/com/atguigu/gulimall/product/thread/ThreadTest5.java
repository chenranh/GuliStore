package com.atguigu.gulimall.product.thread;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//两任务组合 都要完成
public class ThreadTest5 {
    //构造线程池
    public static ExecutorService executor = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        /**
         *
         * 1）applyToEither两个任务有一个执行完成 获取它的返回值，获取感知结果，处理任务并有新的返回值
         *
         *  2）.runAfterEither两个任务有一个执行完成  不需要获取future的结果即使不感知结果，处理任务，没有返回值
         *
         *  3）.AcceptEither 两个任务有一个执行完成 获取感知结果，处理任务，没有新的返回值
         */

        /**
         * 两个任务只要有一个完成 就执行任务三
         */
        System.out.println("main---start");

        CompletableFuture<Object> future1 = CompletableFuture.supplyAsync(() -> {
            System.out.println("任务1线程启动" + Thread.currentThread().getId());
            int i = 20 / 2;
            System.out.println("任务1线程运行结束");
            return i;
        }, executor);


        CompletableFuture<Object> future2 = CompletableFuture.supplyAsync(() -> {
            System.out.println("任务2线程启动" + Thread.currentThread().getId());
            System.out.println("任务2线程运行结束");
            return "hello";
        }, executor);

//---------------------线程3的三种处理方式----------------------------
        //只要两个线程有一个完成就执行线程3
        future1.runAfterEitherAsync(future2, () -> {
            System.out.println("任务3线程开始");
        }, executor);
        System.out.println("main---end");


        //要求任务1和任务2有相同的返回类型，不然res不知道返回哪个线程值的类型
        //res感知到的是线程1的值
        future1.acceptEitherAsync(future2, (res) -> {
            System.out.println("任务3开始"+res);
        }, executor);
        System.out.println("main---end");


        //第三个线程能拿到前两个的返回值 自身也有返回值
        CompletableFuture<String> future = future1.applyToEitherAsync(future2, (res) -> {
            System.out.println("任务3开始--之前的结果"+res);
            return "返回的结果"+res.toString();
        }, executor);


        System.out.println("main---end" + future.get());


    }
}