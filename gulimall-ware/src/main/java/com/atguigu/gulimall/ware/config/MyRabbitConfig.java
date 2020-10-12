package com.atguigu.gulimall.ware.config;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Exchanger;

/**
 * <p>Title: MyRabbitConfig</p>
 * Description：rabbit确认机制配置   序列化方式配置
 */
@Slf4j
@Configuration
public class MyRabbitConfig {

    /**
     * 转换发送消息的序列化方式 改用jackson
     * 发送的消息变成json类型 相当于redis的改变序列化方式
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }


//-------------消息消费者测试-----------------------
//    @RabbitListener(queues = "stock.release.stock.queue")
//    public void handle(Channel channel, Message message) throws IOException {
//    }


    //------------------------------创建一个交换机------------------------------
    /**
     * 创建库存的交换机
     *
     * @return
     */
    @Bean
    public Exchange stockEventExchange() {
        //String name, boolean durable, boolean autoDelete
        return new TopicExchange("stock-event-exchange", true, false);
    }

    //------------------------------创建两个队列-------------------------------

    /**
     * 普通队列
     * @return
     */
    @Bean
    public Queue stockReleaseStockQueue() {
        //String name, boolean durable, boolean exclusive, boolean autoDelete
        return new Queue("stock.release.stock.queue", true, false,false);
    }


    /**
     * 延时队列
     * @return
     */
    @Bean
    public Queue stockDelayQueue() {
        Map<String, Object> arguments = new HashMap<>();
        //死信队列需要设置三个参数
        arguments.put("x-dead-letter-exchange", "stock-event-exchange");//库存死信路由  延迟队列绑定的路由
        arguments.put("x-dead-letter-routing-key", "stock-release");//库存死信路由键 延迟队列绑定的路由的路由键
        arguments.put("x-message-ttl", 120000);//消息过期时间
        //String name, boolean durable, boolean exclusive, boolean autoDelete
        return new Queue("stock.delay.queue", true, false,false,arguments);
    }

    //------------------------------创建两个绑定关系-------------------------------

    /**
     * 和普通消息队列进行绑定
     * @return
     */
    @Bean
    public Binding stockReleaseBinding(){
        return new Binding("stock.release.stock.queue",
                Binding.DestinationType.QUEUE,
                "stock-event-exchange",
                "stock.release.#",
                null);
    }

    /**
     * 和延时队列进行绑定
     * @return
     */
    @Bean
    public Binding stockLockBinding(){
        return new Binding("stock.delay.queue",
                Binding.DestinationType.QUEUE,
                "stock-event-exchange",
                "stock.locked",
                null);
    }

}
