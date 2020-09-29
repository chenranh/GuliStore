package com.atguigu.gulimall.order.service.impl;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderReturnReasonEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.order.dao.OrderItemDao;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.atguigu.gulimall.order.service.OrderItemService;

@RabbitListener(queues = {"hello-java-queue"})
@Service("orderItemService")
public class OrderItemServiceImpl extends ServiceImpl<OrderItemDao, OrderItemEntity> implements OrderItemService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderItemEntity> page = this.page(
                new Query<OrderItemEntity>().getPage(params),
                new QueryWrapper<OrderItemEntity>()
        );

        return new PageUtils(page);
    }



    /**
     * queue声明需要监听的所有队列
     * @param message
     * 参数可以写以下类型
     *  1.Message message 原生消息详细信息 头加体
     *  2.T<发送的消息类型> OrderReturnReasonEntity content
     *  3.Channel channel 当前传输数据的通道
     *
     *  queue：可以很多人都来监听 只要收到消息，队列删除消息，而且只能有一个收到此消息
     *      1）订单服务启动多个 同一条消息只能有一个客户端接收到
     *      2）业务处理期间能不能接收其他消息  只有当前消息处理完才能接收其他消息
     */
    @RabbitHandler
    public void recieveMessage(Message message,Channel channel){
        //消息里的标签 channel内按顺序自增的
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            if(deliveryTag%2==0){
                //签收 ack确认消息 第二个参数multiple 是否批量签收消息
                channel.basicAck(deliveryTag,false);
            }else {
                //拒收消息  第二个参数是否批量拒收，第三个参数是否重新入列
                channel.basicNack(deliveryTag,false,false);
                //同上 但不能批量
                channel.basicReject(deliveryTag,false);
            }
        } catch (IOException e) {
            //网络中断 签收状态发不出去
            e.printStackTrace();
        }
    }

    @RabbitHandler
    public void recieveMessage2(OrderEntity orderEntity){
        System.out.println("接收到的消息内容"+orderEntity);
    }
}
