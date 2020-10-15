package com.atguigu.gulimall.order.web;

import com.alipay.api.AlipayApiException;
import com.atguigu.gulimall.order.config.AlipayTemplate;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.vo.PayVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @title: PayWebController
 * @Author yuke
 * @Date: 2020-10-15 13:40
 */
@Controller
public class PayWebController {
    @Autowired
    private AlipayTemplate alipayTemplate;

    @Autowired
    private OrderService orderService;

    /**
     * 1.将支付页让浏览器展示
     * 2.支付成功以后，我们要跳到用户的订单列表页
     * 告诉浏览器我们会返回一个html页面
     */
    @ResponseBody
    @GetMapping(value = "/payOrder", produces = "text/html")
    public String payOrder(@RequestParam("orderSn") String orderSn) throws AlipayApiException {
        PayVo payVo = orderService.getOrderPay(orderSn);
        //返回的是一个页面，将此页面直接交给浏览器就行，会直接跳转到一个支付页面
        return alipayTemplate.pay(payVo);
    }
}
