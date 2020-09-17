package com.atguigu.gulimall.product.thread;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//线程串行化方法
public class ThreadTest3 {
    public static ExecutorService executor = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("main---start");

        /**
         * 线程串行化
         * 1）theRun不能获取到上一步的执行结果，无返回值
         * .thenRunAsync(() -> {
         *             System.out.println("任务2启动了");
         *         }, executor);
         *
         *  2）.thenAccept 能接受上一步结果，但是无返回值
         *
         *  3）.thenAccept 能接受上一步结果，有返回值
         */
        CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("当前线程" + Thread.currentThread().getId());
            int i = 20 / 2;
            System.out.println("运行结果" + i);
            return i;
            //thenRun:只要上面的任务执行完成后就开始执行therun,带有asyns是异步执行的，不带是在当前线程执行的
        }, executor).thenRunAsync(() -> {
            System.out.println("任务2启动了");
        }, executor);

        System.out.println("main---end---");

//-------------------------------------------------------------------------------------------
        CompletableFuture<Void> future2 = CompletableFuture.supplyAsync(() -> {
            System.out.println("当前线程" + Thread.currentThread().getId());
            int i = 20 / 2;
            System.out.println("运行结果" + i);
            return i;
            //res是上一个线程的返回结果 thenAccept能接受上一步结果，但是无返回值
        }, executor).thenAcceptAsync((res) -> {
            System.out.println("任务2启动了"+res);
        }, executor);

        System.out.println("main---end---");

        //-------------------------------------------------------------------------------------------
        CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> {
            System.out.println("当前线程" + Thread.currentThread().getId());
            int i = 20 / 2;
            System.out.println("运行结果" + i);
            return i;
            //res是上一个线程的返回结果 thenApply能接受上一步结果，有返回值 "hello" + res
        }, executor).thenApplyAsync(res -> {
            System.out.println("任务2启动了" + res);

            return "hello" + res;
        }, executor);
        //future3.get() 是一个阻塞方法
        System.out.println("main---end---"+future3.get());
    }
}
