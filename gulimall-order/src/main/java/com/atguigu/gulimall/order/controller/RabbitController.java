package com.atguigu.gulimall.order.controller;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderReturnReasonEntity;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.UUID;

/**
 * @title: RabbitController
 * @Author yuke
 * @Date: 2020-09-28 21:41
 */
@RestController
public class RabbitController {
    @Autowired
    RabbitTemplate rabbitTemplatet;

    /**
     * 测试生产者 发送消息
     * @param num
     * @return
     */
    @GetMapping("/sendMq")
    public String sendMq(@RequestParam(value = "num", defaultValue = "10") Iterable num) {
        for (int i = 0; i < 10; i++) {
            if (i % 2 == 0) {
                OrderReturnReasonEntity reasonEntity = new OrderReturnReasonEntity();
                reasonEntity.setId(1L);
                reasonEntity.setCreateTime(new Date());
                reasonEntity.setName("哈哈" + i);
                //new CorrelationData(UUID.randomUUID().toString())消息的唯一id
                rabbitTemplatet.convertAndSend("hello-java-exchange", "hello-java", reasonEntity,new CorrelationData(UUID.randomUUID().toString()));
            } else {
                OrderEntity orderEntity = new OrderEntity();
                orderEntity.setOrderSn(UUID.randomUUID().toString());
                rabbitTemplatet.convertAndSend("hello-java-exchange", "hello-java", orderEntity,new CorrelationData(UUID.randomUUID().toString()));
            }

        }
        return "ok";
    }
}
