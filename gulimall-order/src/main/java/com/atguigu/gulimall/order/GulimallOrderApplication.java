package com.atguigu.gulimall.order;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 使用rqbbitmq
 * 1.引入amqp依赖 rabbitautoconfiguration自动生效
 * 2.自动配置
 * 3.yml文件配置以spring.rabbitmq开头的配置
 * 4.@enableRabbit
 *
 */

@EnableRabbit //开启mq
@SpringBootApplication
@EnableDiscoveryClient
public class GulimallOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallOrderApplication.class, args);
    }

}
