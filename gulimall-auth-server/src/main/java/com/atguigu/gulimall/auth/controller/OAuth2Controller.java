package com.atguigu.gulimall.auth.controller;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.auth.feign.MemberFeignService;
import com.atguigu.common.vo.MemberRsepVo;
import com.atguigu.gulimall.auth.vo.SocialUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 处理社交登录请求
 * 社交登录 微博登录
 * 调用远程会员登录接口
 */
@Controller
@Slf4j
public class OAuth2Controller {

    @Autowired
    MemberFeignService memberFeignService;

    @GetMapping("oauth2.0/weibo/success")
    public String weibo(@RequestParam("code") String code, HttpSession session) throws Exception {
        HashMap<String, String> map = new HashMap<>();
        map.put("client_id", "992647046");
        map.put("client_secret", "5ea0d1785eaf5a3baa892c97027d6c55");
        map.put("grant_type", "authorization_code");
        map.put("redirect_uri", "http://auth.gulimall.com/oauth2.0/weibo/success");
        map.put("code", code);
        Map<String, String> headers = new HashMap<>();
        //1. 根据code换取accessToken
        HttpResponse response = HttpUtils.doPost("https://api.weibo.com", "/oauth2/access_token", "post", headers, null, map);
        //2. 处理
        if (response.getStatusLine().getStatusCode() == 200) {
            //获取到了accessToken
            String json = EntityUtils.toString(response.getEntity());
            SocialUser socialUser = JSON.parseObject(json, SocialUser.class);
            //知道了当前是哪个社交用户
            // 1）当前用户如果是第一次进网站，自动注册进来（为当前用户生成一个会员信息账号，以后这个账号就对应指定的会员）
            //登录或者注册这个社交用户  社交用户一定要关联本系统的账号信息
            R oathlogin = memberFeignService.login(socialUser);
            if (oathlogin.getCode() == 0) {
                MemberRsepVo data = oathlogin.getData("data", new TypeReference<MemberRsepVo>() {});
                // 第一次使用session 命令浏览器保存这个用户信息 JESSIONSEID 每次只要访问这个网站就会带上这个cookie
                // 在发卡的时候扩大session作用域 (指定域名为父域名)
                // TODO 1.默认发的当前域的session (需要解决子域session共享问题)
                // TODO 2.使用JSON的方式序列化到redis
                session.setAttribute(AuthServerConstant.LOGIN_USER, data);
                log.info("登录成功：用户：{}",data.toString());
                //log.info("\n欢迎 [" + data.getUsername() + "] 使用社交账号登录");
                //2. 登陆成功就跳回首页
                return "redirect:http://gulimall.com";
            } else {
                return "redirect:http://auth.gulimall.com/login.html";
            }

        } else {
            return "redirect:http://auth.gulimall.com/login.html";
        }

    }
}
