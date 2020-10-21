package com.atguigu.gulimall.seckill;

import org.redisson.spring.session.config.EnableRedissonHttpSession;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 1.整合sentinel
 *    1）导入依赖 spring-cloud-starter-alibaba-sentinel
 *    2）下载sentinel的控制台 java -jar sentinel.jar启动
 *    3）配置sentinel控制台地址信息
 *    4) 在控制台调整参数【默认所有的流控设置保存在内存中，重启后丢失】
 *
 *  2.实时监控需要Endpoint支持，每个微服务导入actuator依赖 配置management.endpoints.web.exposure.include=*
 *
 *  3.自定义sentinel自定义返回数据  SecKillSentinelConfig
 *
 *  4.使用sentinel来保护feign远程调用：熔断
 *    1）调用方的熔断保护  feign.sentinel.enabled=true
 *    2）调用方手动在sentinel控制台指定远程服务的降级策略  远程服务降级以后会触发熔断保护，不再调用
 *    3）超大流量浏览的时候，必须牺牲一些远程服务。在服务的提供方（远程服务）指定降级策略
 *      提供方是在运行，但是不运行自己的业务逻辑，返回的是默认的降级数据（SecKillSentinelConfig里的限流数据）
 *
 *
 */


//@EnableRabbit 不监听消息，可以不用这个注解
@EnableRedissonHttpSession
@EnableFeignClients(basePackages = "com.atguigu.gulimall.seckill.feign")
@EnableDiscoveryClient
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class}) //排除数据库部分
public class GulimallSeckillApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallSeckillApplication.class, args);
    }

}
