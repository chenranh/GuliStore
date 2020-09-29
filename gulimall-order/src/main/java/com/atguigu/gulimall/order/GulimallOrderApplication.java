package com.atguigu.gulimall.order;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
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
 */


@EnableRabbit //开启mq
@SpringBootApplication
@EnableDiscoveryClient
@EnableRedisHttpSession
@EnableFeignClients
public class GulimallOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallOrderApplication.class, args);
    }

}
