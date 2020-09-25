package com.atguigu.gulimallcart.controller;

import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.gulimallcart.interceptor.CartInterceptor;
import com.atguigu.gulimallcart.service.CartService;
import com.atguigu.gulimallcart.vo.CartItem;
import com.atguigu.gulimallcart.vo.UserInfoTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.concurrent.ExecutionException;

@Controller
public class CartController {

    @Autowired
    CartService cartService;

    /**
     * 添加商品到购物车
     * @return
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num, Model model) throws ExecutionException, InterruptedException {
        CartItem cartItem=cartService.addToCart(skuId,num);
        model.addAttribute("item", cartItem);
        return "success";
    }












    /**
     * 浏览器有一个cookie：user-key标识用户身份，一个月后过期
     * 如果第一次使用jd的购物车功能 都会有一个临时的用户身份
     * 浏览器以后保存，每次访问都会带上这个cookie
     *
     * 登录：session中有
     * 没登陆：按照cookie里面带来user-key来做
     * 第一次如果没有临时用户 帮忙创建一个临时用户
     * @return
     */
    @GetMapping("/cart.html")
    public String cartListPage(){

        //1.快速得到用户信息：id、user-key
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();


        return "cartList";
    }


}
