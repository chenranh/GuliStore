package com.atguigu.gulimall.order.testTransactional;

import com.atguigu.gulimall.order.service.impl.OrderServiceImpl;
import org.springframework.aop.framework.AopContext;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @title: 事务注解学习
 * @Author yuke
 * @Date: 2020-10-09 7:54
 */
public class test {

    //同一个对象内事务方法互相调用默认失效，原因是绕过了代理对象
    //事务使用代理对象来控制的
    @Transactional(timeout = 30)//a事务的所有设置传播到了和他共用一个事务的方法
    public void a() {
        //bc做任何设置都没有用 都是和a共用一个事务
        b();//a事务
        c();//新事物（不回滚）
        int i = 10 / 0;
        //想要b c的设置生效，使用代理对象（参照启动类里的信息）
        test orderService = (test) AopContext.currentProxy();
        orderService.c();
        orderService.b();


    }

    @Transactional(propagation = Propagation.REQUIRED)//表示和调用b方法的方法共用同一个事务
    public void b() {
        //7s
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)//表示使用新事务
    public void c() {

    }

}
