package com.atguigu.gulimall.order;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.lang.Nullable;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
class GulimallOrderApplicationTests {

    @Autowired
    AmqpAdmin amqpAdmin;


    /**
     * 1.如何创建exchange queue binding
     * 使用 amqpAdmin进行创建
     * 2.如何收发消息
     */

//========================================================创建部分==========================================================
    //创建交换机
    @Test
    void createExchange() {
//        DirectExchange(String name, boolean durable, boolean autoDelete, Map<String, Object> arguments)

        DirectExchange directExchange = new DirectExchange("hello-java-exchange", true, false);
        amqpAdmin.declareExchange(directExchange);
        log.info("Exchange{}创建成功", "hello-java-exchange");
    }

    //创建队列
    @Test
    void createQueue() {
//        Queue(String name, boolean durable, boolean exclusive, boolean autoDelete, @Nullable Map<String, Object> arguments)
//        exclusive参数 表示只能有一个能连接到队列
        Queue queue = new Queue("hello-java-queue", true, false, false);
        amqpAdmin.declareQueue(queue);
        log.info("Queue{}创建成功", "hello-java-queue");
    }


    //创建绑定关系
    @Test
    void createBinding() {
//        Binding(String destination, 目的地 队列
//        Binding.DestinationType destinationType, 目的地类型
//        String exchange,交换机
//        String routingKey,路由键
//        @Nullable Map<String, Object> arguments) 自定义参数


        //将交换机和目的地进行绑定 使用routingKey作为路由键
        Binding binding = new Binding("hello-java-queue",
                Binding.DestinationType.QUEUE,
                "hello-java-exchange",
                "hello-java", null);
        amqpAdmin.declareBinding(binding);
        log.info("Binding{}创建成功", "hello-java-binding");
    }
    //================================================收发消息=================================================


}
