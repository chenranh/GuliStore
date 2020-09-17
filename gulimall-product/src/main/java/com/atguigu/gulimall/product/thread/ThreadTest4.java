package com.atguigu.gulimall.product.thread;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//两任务组合 都要完成
public class ThreadTest4 {
    //构造线程池
    public static ExecutorService executor = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        /**
         *
         * 1）theCombine组合两个future 获取两个future的返回结果，并返回当前任务的返回值
         *      .thenRunAsync(() -> {
         *               System.out.println("任务2启动了");
         *         }, executor);
         *
         *  2）.runAfterBoth 组合两个future 不需要获取future的结果 只需两个future处理完任务后 处理该任务
         *
         *  3）.thenAcceptBoth 组合两个future 获取两个future任务的返回结果 然后处理任务 没有返回值
         */

        /**
         * 两个都完成
         */
        System.out.println("main---start");

        CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> {
            System.out.println("任务1线程启动" + Thread.currentThread().getId());
            int i = 20 / 2;
            System.out.println("任务1线程运行结束");
            return i;
        }, executor);


        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            System.out.println("任务2线程启动" + Thread.currentThread().getId());
            System.out.println("任务2线程运行结束");
            return "hello";
        }, executor);

        //线程3在线程1和2完成后执行
        future1.runAfterBothAsync(future2, () -> {
            System.out.println("任务3线程开始");
        }, executor);
        System.out.println("main---end");


        //第二个参数 参数类型 是 void accept(T t, U u);
        //第三个线程能拿到前两个的返回值 但自身没有返回值
        future1.thenAcceptBothAsync(future2, (f1, f2) -> {
            System.out.println("任务3开始---之前的结果：" + f1 + "---" + f2);
        }, executor);
        System.out.println("main---end");

        //第三个线程能拿到前两个的返回值 自身也有返回值
        CompletableFuture<String> future = future1.thenCombineAsync(future2, (f1, f2) -> {
            System.out.println("任务3开始---之前的结果：" + f1 + "---" + f2);
            return "f1" + f1 + "---" + "f2" + f2;
        }, executor);

        System.out.println("main---end" + future.get());


    }
}