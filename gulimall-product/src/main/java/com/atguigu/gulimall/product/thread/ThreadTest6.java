package com.atguigu.gulimall.product.thread;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//多任务组合
public class ThreadTest6 {
    //构造线程池
    public static ExecutorService executor = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        /**
         *
         * 1）allOf 等待所有任务完成
         *
         *  2）.anyOf 只要有一个任务完成

         */

        System.out.println("main---start");

        CompletableFuture<String> futureImg = CompletableFuture.supplyAsync(() -> {
            System.out.println("查询商品的图片信息");
            return "hello.jpg";
        }, executor);

        CompletableFuture<String> futureAttr = CompletableFuture.supplyAsync(() -> {
            System.out.println("查询商品的属性信息");
            return "黑色+256G";
        }, executor);


        CompletableFuture<String> futureDesc = CompletableFuture.supplyAsync(() -> {
            System.out.println("查询商品介绍");
            return "华为";
        }, executor);


        CompletableFuture<Void> allOf = CompletableFuture.allOf(futureImg, futureAttr, futureDesc);
        //等待所有结果完成，主线程才能完成
        allOf.get();
        System.out.println("main---end"+futureImg.get()+futureAttr.get()+futureDesc.get());

        //只要一个完成 主线程就能执行
        CompletableFuture<Object> anyOf = CompletableFuture.anyOf(futureImg, futureAttr, futureDesc);
        anyOf.get();
        System.out.println("main---end"+anyOf.get());

    }
}