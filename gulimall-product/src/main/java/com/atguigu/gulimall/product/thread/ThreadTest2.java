package com.atguigu.gulimall.product.thread;



import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 异步编排CompletableFuture
 * 4. 线程池
 * 给线程池直接提交任务
 * service.execute(new Runable01());
 * 1.创建
 * 1）juc包下 exectors 最快创建方式 Executors.newFixedThreadPool(10);
 * 2） new ThreadPoolExecutor();
 * <p>
 * Future可以获取异步结果
 */
public class ThreadTest2 {

    public static ExecutorService executor = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("main---start");
        //第一个参数直接是异步内容，相当于之前run方法里的代码,没有返回值
//        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
//            System.out.println("当前线程" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("运行结果" + i);
//        }, executor);


        //Supplier的get方法不接受入参（读不懂看底层），所以() ->里没有入参，supplyAsync有返回值
        //whenComplete有带async和不带async，不带就是在当前线程继续执行，带了会重新整一个线程进行处理
        /**
         * 方法执行完成后的感知
         */
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("当前线程" + Thread.currentThread().getId());
            int i = 20 / 2;
            System.out.println("运行结果" + i);
            return i;
        }, executor).whenComplete((result,exception)->{
            //whenComplete虽然能得到异常信息，但是无法修改返回数据
            System.out.println("异步任务完成了--结果是："+result+";异常是："+exception);
            //R apply(T t)  exceptionally()的参数是抛出来的异常
        }).exceptionally(throwable -> {
            //exceptionally可以感知异常，同时返回默认值
            return 10;
        });

        /**
         * handle()方法 执行完成后的处理
         */
        CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> {
            System.out.println("当前线程" + Thread.currentThread().getId());
            int i = 20 / 2;
            System.out.println("运行结果" + i);
            return i;
            // R apply(T t, U u); u是异常参数
        }, executor).handle((res,thr)->{
            if (res!=null){
                return res*2;
            }
            //表示有异常
            if (thr!=null){
                return 0;
            }
            return res;
        });

        Integer i = future.get();
        System.out.println("main---end---"+i);
    }
}
