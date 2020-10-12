package com.atguigu.gulimall.ware.listener;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.mq.StockDetailTo;
import com.atguigu.common.to.mq.StockLockedTo;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.atguigu.gulimall.ware.entity.WareOrderTaskEntity;
import com.atguigu.gulimall.ware.service.WareSkuService;
import com.atguigu.gulimall.ware.vo.OrderVo;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * <p>Title: StockReleaseListener</p>
 * Description：
 * date：2020/7/3 23:56
 */
@Service
@RabbitListener(queues = "stock.release.stock.queue")
public class StockReleaseListener {

	@Autowired
	private WareSkuService wareSkuService;

	/**
	 * 1.库存自动解锁
	 * 下订单成功，库存锁定成功，但是接下来的业务调用失败导致订单回滚 之前锁定的库存就要自动解锁
	 * 2.订单失败
	 *    锁库存失败
	 * 3.只要解锁库存的消息失败，一定要告诉服务解锁失败 启用手动解锁模式
	 *
	 * @param to
	 * @param message
	 */
	@RabbitListener
	public void handleStockLockRelease(StockLockedTo to, Message message, Channel channel) throws IOException {
		System.out.println("收到解锁库存的消息");
		try {
			wareSkuService.unlockStock(to);
			//消费端 消息执行成功ack确认
			channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
		} catch (Exception e) {
			//当前消息执行失败 重新回队
			channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
		}

	}
}