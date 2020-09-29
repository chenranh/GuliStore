package com.atguigu.gulimall.order.web;

import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.vo.OrderConfirmVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.concurrent.ExecutionException;

/**
 * @title: OrderWebController
 * @Author yuke
 * @Date: 2020-09-29 17:55
 */
@Controller
public class OrderWebController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/toTrade")
    public String toTrade(Model model) {

       OrderConfirmVo confirmVo = orderService.confirmOrder();

       model.addAttribute("orderConfirmData", confirmVo);
        return "confirm";
    }





}
