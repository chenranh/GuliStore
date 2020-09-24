package com.atguigu.gulimallcart.controller;

import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.gulimallcart.interceptor.CartInterceptor;
import com.atguigu.gulimallcart.vo.UserInfoTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CartController {


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
