package com.atguigu.gulimall.product.thread;


import java.util.concurrent.*;

/**
 * 用Thread和Runable代码不用等到另一个线程里的流程走完才走下面的
 * main---start
 * main---end---
 * 当前线程11
 * 运行结果5
 * 当前线程12
 * 运行结果10
 * Callable是阻塞等待需要另一个线程执行完才走下面的代码
 * main---start
 * 当前线程11
 * 运行结果5
 * 当前线程12
 * 运行结果10
 * 当前线程13
 * 运行结果10
 * main---end---10
 */
public class ThreadTest {

    /**
     * 1.继承thread
     *      Thread01 thread01=new Thread01();
     *         thread01.start(); //启动线程
     * 2.实现runnable接口
     *      Runable01 runable01 = new Runable01();
     *         new Thread(runable01).start(); //启动线程
     * 3.实现callable接口+funturetask 可以拿到返回结果 可以处理异常
     *  阻塞等待整个线程执行完成，获取返回结果
     *     FutureTask<Integer> futureTask = new FutureTask<>(new Callable01());
     *          new Thread(futureTask).start(); //启动线程
     * 4. 线程池
     *       给线程池直接提交任务
     *       service.execute(new Runable01());
     *       1.创建
     *         1）juc包下 exectors 最快创建方式 Executors.newFixedThreadPool(10);
     *         2） new ThreadPoolExecutor();
     *
     *
     *
     * 区别：
     *  1、2不能得到返回值，3可以获取返回值
     *  1、2、3都不能控制资源  只能通过new Thread占用系统内存
     *  4、可以控制资源  直接初始化10个线程 资源循环使用 资源稳定不会因为高并发导致资源耗尽的问题
     *
     *
     * 问：为什么需要创建线程池？频繁创建线程会很占用系统资源
     */
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("main---start");

        //其他三种启动线程的方式都不使用，将所有的多线程异步任务都交给线程池执行
        //new Thread(()-> System.out.println("hello")).start();

        //当前系统中池只有一两个，每个异步任务提交给线程池让他自己去执行就行
        //1.通过Executors.newFixedThreadPool创建线程池   Executors在juc包下
        ExecutorService service = Executors.newFixedThreadPool(10);
        service.execute(new Runable01());

        /**
         * Executors的其他几种创建方式
         */
        //没有核心线程，但是可以创建很多的线程，线程空闲都会清空到0，所有的都可以回收
        Executors.newCachedThreadPool();
        //固定的核心线程数，一直存活，所有的都不可以回收
        Executors.newFixedThreadPool(10);
        //用来做定时任务的线程池
        Executors.newScheduledThreadPool(10);
        //单线程的线程池，后台从队列中获取任务，挨个执行，只有一个核心线程
        Executors.newSingleThreadExecutor();



        //2.通过new ThreadPoolExecutor()创建线程池
        /**
         * 七大参数
         * 1.corePoolSize:[5] 核心线程数[一直存在除非（allowCoreThreadTimeOut）] 线程池创建好以后就准备就绪的线程数量。就等待来接受异步任务去执行
         *      5个 Thread01 thread01=new Thread01();  thread01.start()
         *
         * 2.maximumPoolSize:最大线程数量；控制资源
         *
         * 3.keepAliveTime:存活时间。如果当前的线程数量大于核心数量
         *      释放空闲的线程（maximumPoolSize-corePoolSize的部分，核心的不释放）  只要线程空闲大于指定的存活时间
         *
         * 4.unit 时间单位
         *
         * 5.BlockingQueue<Runnable> workQueue)：阻塞队列。如果任务有很多，就会将目前多的任务放在队列里面
         *          只要又线程空闲了，就会去队列里取出新的任务继续执行
         *  new LinkedBlockingDeque<>():默认保存是integer的最大值。最大值缺点可能会造成内存不够
         *
         *  6.threadFactory 线程的创建工厂。
         *
         *  7.RejectedExecutionHandler handler 如果队列满了 按照我们指定的拒绝策略拒绝执行任务
         *
         *  工作顺序：
         *  1.线程池创建，准备好core数量的核心线程，准备接受任务
         *     1.1、core满了,就将再进来的任务放入阻塞队列中。空闲的core就会自己去阻塞队列获取任务执行
         *     1.2、阻塞队列满了。就直接开新的线程执行，最大只能开到max指定的数量
         *     1.3、max满了就用 RejectedExecutionHandler拒绝任务
         *     1.4 max都执行完成，有很多空闲，在指定的时间keepAliveTime，释放（maximumPoolSize-corePoolSize）个线程
         *
         *  面试：一个线程池 core 7：max 20 ,queue:50 ,100并发进来怎么分配的
         *      7个会立即得到执行，50个会进入队列，再开13个进行执行。剩下的30个使用拒绝策略。
         *      如果不想抛弃使用CallerRunsPolicy拒绝策略，以同步的方式执行，或者丢弃最老的
         */
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(5,
                200,
                10,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(100000),
                //默认线程工程
                Executors.defaultThreadFactory(),
                //拒绝策略
                new ThreadPoolExecutor.AbortPolicy());

        threadPoolExecutor.execute(new Runable01());


        System.out.println("main---end---");

    }

    public static class Thread01 extends Thread {
        @Override
        public void run() {
            System.out.println("当前线程" + Thread.currentThread().getId());
            int i = 10 / 2;
            System.out.println("运行结果" + i);
        }
    }

    public static class Runable01 implements Runnable {

        @Override
        public void run() {
            System.out.println("当前线程" + Thread.currentThread().getId());
            int i = 20 / 2;
            System.out.println("运行结果" + i);
        }
    }


    public static class Callable01 implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            System.out.println("当前线程" + Thread.currentThread().getId());
            int i = 20 / 2;
            System.out.println("运行结果" + i);
            return i;
        }
    }


}
