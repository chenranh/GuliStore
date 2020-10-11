package com.atguigu.gulimall.order.web;

import com.atguigu.gulimall.order.entity.OrderEntity;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.UUID;

/**
 * @title: HelloController
 * @Author yuke
 * @Date: 2020-09-29 16:09
 */
@Controller
public class HelloController {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @GetMapping("/{page}.html")
    public String listPage(@PathVariable("page") String page){

        return page;
    }

    @ResponseBody
    @GetMapping("/test/createOrder")
    public String createOrderTest(){

        //订单下单成功
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(UUID.randomUUID().toString());
        //给mq发送消息
        rabbitTemplate.convertAndSend("order-event-exchange","order-create-order",orderEntity);

        return "ok";
    }



}
