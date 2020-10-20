package com.atguigu.gulimall.seckill.config;//package com.atguigu.gulimall.order.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * 2020-10-14 引用别人更好的配置，对消息丢失做了处理存入数据库，在OrderServiceImpl中对给mq发送的消息重写了id，对应数据库的message_id
 * 写的很好 respec
 * RabbitMQ全局配置
 *
 * @author 孙启新
 * <br>FileName: MyRabbitMqGlobalConfig
 * <br>Date: 2020/08/07 14:41:44
 */
@Configuration
@Slf4j
public class MyRabbitConfig {

    /**
     * 转换发送消息的序列化方式 改用jackson
     * 发送的消息变成json类型 相当于redis的改变序列化方式
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

}
