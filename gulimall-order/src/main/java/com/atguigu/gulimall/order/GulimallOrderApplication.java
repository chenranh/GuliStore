package com.atguigu.gulimall.order;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 使用rqbbitmq
 * 1.引入amqp依赖 rabbitautoconfiguration自动生效
 * 2.自动配置
 * 3.yml文件配置以spring.rabbitmq开头的配置
 * 4.@enableRabbit
 * 5.监听消息 使用@RabbitListener; 必须有@EnableRabbit
 * 6 @RabbitListener 可以加在类和和方法上 监听哪些队列
 * 7 @RabbitHandler 加在方法上 可以重载区分不同的消息
 *
 * 本地事务失效问题
 *  同一个对象事务方法默认失效, 原因 绕过了代理对象 事务使用代理对象来控制的
 *   解决：使用代理对象来调用事务方法
 *   	1. 引入 spring-boot-starter-aop 它帮我们引入了aspectj
 *   	2. @EnableAspectJAutoProxy(exposeProxy = true) [对外暴露代理对象] 开启动态代理功能 而不是jdk默认的动态代理 即使没有接口也可以创建动态代理
 * 		3. 本类互调用代理对象		AopContext.currentProxy()
 *
 * seata控制分布式事务  senta at模式（2pc模式）不适合高并发，加的锁太多
 *  1）每一个微服务先必须创建undo_log
 *  2）下载安装事务协调器 seata-server
 *     1.common服务 导入依赖spring-cloud-alibaba-seata  seata-all 1.0
 *     2.启动seata-server 分布式事务里的协调器
 *          registry.conf注册中心配置  type = "nacos"
 *     3.所有想要用到分布式的事务的微服务使用seata datasourceproxy代理自己的数据源 配置文件配置代理数据源
 *     4.每个微服务都必须导入 file.conf  registry.conf
 *     file.conf中更改vgroup_mapping名称 当前服务名加fescar-service-group
 *    5.启动服务测试
 *    6.核心 给分布式大事务入口标注@GlobalTransactional
 *    7.每个远程的小事务用@Transactional
 *
 *
 *
 *
 */

@EnableAspectJAutoProxy(exposeProxy = true)//开启动态代理功能，对外暴露代理对象
@EnableRabbit //开启mq
@SpringBootApplication
@EnableDiscoveryClient
@EnableRedisHttpSession
@EnableFeignClients(basePackages = "com.atguigu.gulimall.order.feign")
public class GulimallOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallOrderApplication.class, args);
    }

}
