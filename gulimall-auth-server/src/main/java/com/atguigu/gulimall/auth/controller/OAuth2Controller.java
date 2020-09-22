package com.atguigu.gulimall.auth.controller;

/**
 * @title: OAuth2Controller
 * @Author yuke
 * @Date: 2020-09-22 11:37
 */

import com.alibaba.fastjson.JSON;
import com.atguigu.common.utils.HttpUtils;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

/**
 * 处理社交登录请求
 * 微博登录
 */
@Controller
public class OAuth2Controller {

    @GetMapping("oauth2.0/weibo/success")
    public String weibo(@RequestParam("code") String code) throws Exception {
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
        if (response.getStatusLine().getStatusCode()==200) {
            //获取到了accessToken
            String json = EntityUtils.toString(response.getEntity());
            SocialUser socialUser = JSON.parseObject(json, SocialUser.class);
            //知道了当前是哪个社交用户
            // 1）当前用户如果是第一次进网站，自动注册进来（为当前用户生成一个会员信息账号，以后这个账号就对应指定的会员）
            //登录或者注册这个社交用户  社交用户一定要关联本系统的账号信息
        }else{
            return "redirect:http://auth.gulimall.com/login.html";
        }
        //2. 登陆成功就跳回首页
        return "redirect:http://gulimall.com";
    }
}
