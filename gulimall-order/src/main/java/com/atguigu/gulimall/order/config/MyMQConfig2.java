package com.atguigu.gulimall.order.config;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 创建两个队列和一个交换机，两个绑定关系  参考延迟死信队列升级图
 * Description：容器中的所有bean都会自动创建到RabbitMQ中 [RabbitMQ没有这个队列、交换机、绑定]
 * 相当于GulimallOrderApplicationTests类里的创建消息队列，路由，绑定消息队列和路由
 */
@Configuration
public class MyMQConfig2 {


//-------------------测试 消息在HelloController发送---------------------

    @RabbitListener(queues = "order.release.order.queue")
    public void listener(OrderEntity orderEntity, Channel channel, Message message) throws IOException {
        System.out.println("收到过期的订单信息：准备关闭订单"+orderEntity.getOrderSn());
        //消费端手动收到消息确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

//--------------------------------两个队列-------------------------------
    /**
     * String name, boolean durable, boolean exclusive, boolean autoDelete,  @Nullable Map<String, Object> arguments
     * 延迟队列
     */
    @Bean
    public Queue orderDelayQueue() {
        Map<String, Object> arguments = new HashMap<>();
        //死信队列需要设置三个参数
        arguments.put("x-dead-letter-exchange", "order-event-exchange");//订单死信路由  延迟队列绑定的路由
        arguments.put("x-dead-letter-routing-key", "order-release-order");//订单死信路由键 延迟队列绑定的路由的路由键
        arguments.put("x-message-ttl", 60000);//消息过期时间
        Queue queue = new Queue("order.delay.queue", true, false, false, arguments);
        return queue;
    }

    /**
     * 普通的队列
     * @return
     */
    @Bean
    public Queue orderReleaseOrderQueue() {
        Queue queue = new Queue("order.release.order.queue", true, false, false);
        return queue;
    }

//--------------------------------一个交换机-------------------------------

    /**
     * String name, boolean durable, boolean autoDelete, Map<String, Object> arguments
     *使用TopicExchange交换机  模糊匹配
     * @return
     */
    @Bean
    public Exchange orderEventExchange() {
        return new TopicExchange("order-event-exchange", true, false);
    }


//--------------------------------两个绑定关系-----------------------------------------------

    /**
     * String destination, DestinationType destinationType, String exchange, String routingKey, @Nullable Map<String, Object> arguments
     * 第一个绑定 交换机和延迟队列的绑定
     */
    @Bean
    public Binding orderCreateOrderBinding() {

        return new Binding("order.delay.queue", Binding.DestinationType.QUEUE, "order-event-exchange", "order-create-order", null);
    }

    /**
     * 第二个绑定  交换机和普通队列的绑定
     * @return
     */
    @Bean
    public Binding orderReleaseOrderBinding() {

        return new Binding("order.release.order.queue", Binding.DestinationType.QUEUE, "order-event-exchange", "order-release-order", null);
    }

}
