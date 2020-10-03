package com.atguigu.gulimall.order.web;

import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.vo.OrderConfirmVo;
import com.atguigu.gulimall.order.vo.OrderSubmitVo;
import com.atguigu.gulimall.order.vo.SubmitOrderResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    /**
     * 去结算确认页
     * @param model
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @GetMapping("/toTrade")
    public String toTrade(Model model) throws ExecutionException, InterruptedException {

       OrderConfirmVo confirmVo = orderService.confirmOrder();

       model.addAttribute("orderConfirmData", confirmVo);
       //展示订单确认的数据
        return "confirm";
    }

    /**
     * 下单功能
     * 下单：去创建订单，验证令牌，验证价格，锁库存...
     * 下单成功来到支付选择页
     * 下单失败回到订单确认页重新确认订单信息
     */
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo submitVo, Model model, RedirectAttributes redirectAttributes){

        try {
            SubmitOrderResponseVo responseVo = orderService.submitOrder(submitVo);
            // 下单失败回到订单重新确认订单信息
            if(responseVo.getCode() == 0){
                // 下单成功取支付选项
                model.addAttribute("submitOrderResp", responseVo);
                return "pay";
            }else{
                String msg = "下单失败";
                switch (responseVo.getCode()){
                    case 1: msg += "订单信息过期,请刷新在提交";break;
                    case 2: msg += "订单商品价格发送变化,请确认后再次提交";break;
                    case 3: msg += "商品库存不足";break;
                }
                redirectAttributes.addFlashAttribute("msg", msg);
                return "redirect:http://order.gulimall.com/toTrade";
            }
        } catch (Exception e) {
            if (e instanceof NotStock Exception){
                String message = e.getMessage();
                redirectAttributes.addFlashAttribute("msg", message);
            }
            return "redirect:http://order.gulimall.com/toTrade";
        }
    }



}
