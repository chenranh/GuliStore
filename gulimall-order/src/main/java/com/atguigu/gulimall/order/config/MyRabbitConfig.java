package com.atguigu.gulimall.order.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * <p>Title: MyRabbitConfig</p>
 * Description：rabbit确认机制配置   序列化方式配置
 */
@Slf4j
@Configuration
public class MyRabbitConfig {

    //	@Autowired
    private RabbitTemplate rabbitTemplate;

    @Primary
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        this.rabbitTemplate = rabbitTemplate;
        rabbitTemplate.setMessageConverter(messageConverter());
        initRabbitTemplate();
        return rabbitTemplate;
    }

    /**
     * 转换发送消息的序列化方式 改用jackson
     * 发送的消息变成json类型 相当于redis的改变序列化方式
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 1.设置确认回调： ConfirmCallback 只要抵达broker就会ack=true
     * 先在配置文件中开启 publisher-confirms: true
     *
     * @PostConstruct: MyRabbitConfig对象创建完成以后 执行这个方法
     *
     * 2.消息抵达队列的确认回调
     * 　	开启发送端消息抵达队列确认
     *       publisher-returns: true
     *      只要抵达队列，以异步优先回调我们这个 returnconfirm
     *      template:
     *      mandatory: true
     *
     * 3.消费端确认(保证每一个消息被正确消费才可以broker删除消息)
     *    1.默认是自动确认的 只要消息接收到 服务端就会移除这个消息
     *
     *      如何签收:
     *      签收: channel.basicAck(deliveryTag, false);
     *      拒签: channel.basicNack(deliveryTag, false,true);
     *      配置文件中一定要加上这个配置
     *      listener:
     *      simple:
     *      acknowledge-mode: manual
     */
//	@PostConstruct //MyRabbitConfig对象创建完以后，执行这个方法
    public void initRabbitTemplate() {
        /**
         * 	设置确认回调  消息是否发送到broker
         *  correlationData: 消息的唯一id
         *  ack： 消息是否成功收到
         * 	cause：失败的原因
         */
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> log.info("\n收到消息: " + correlationData + "\tack: " + ack + "\tcause： " + cause));

        /**
         * 设置消息抵达队列回调：可以很明确的知道那些消息失败了
         *只要消息没有投递给指定的队列，就触发这个失败回调，投递成功就不会回调
         * message: 投递失败的消息详细信息
         * replyCode: 回复的状态码
         * replyText: 回复的文本内容
         * exchange: 当时这个发送给那个交换机
         * routerKey: 当时这个消息用那个路由键
         */
        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routerKey) -> log.error("Fail Message [" + message + "]" +
                "replyCode: " + replyCode +
                "replyText:" + replyText + "exchange:" + exchange + "routerKey:" + routerKey));
    }
}
