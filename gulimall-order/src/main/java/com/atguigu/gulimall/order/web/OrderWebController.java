package com.atguigu.gulimall.order.web;

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

    @GetMapping("/toTrade")
    public String toTrade() {
//        OrderConfirmVo confirmVo = orderService.confirmOrder();
//
//        model.addAttribute("orderConfirmData", confirmVo);
        return "confirm";
    }
}
