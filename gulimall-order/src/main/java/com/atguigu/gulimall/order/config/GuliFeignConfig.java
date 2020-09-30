package com.atguigu.gulimall.order.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 注意是登录状态下！！！ feign远程调用丢失请求头原因：fegin在远程调用之前要构造请求，调用很多的拦截器RequestInterceptor
 * 自己服务没有拦截器就会新new一个 丢掉远程服务请求头里的内容
 * feign远程调用丢失请求头内容问题解决  添加自己的拦截器
 */
@Configuration
public class GuliFeignConfig {

    @Bean("requestInterceptor")
    public RequestInterceptor requestInterceptor() {
        // Feign在远程调用之前都会先经过这个方法
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // RequestContextHolder拿到刚进来这个请求  远程服务器发过来的请求
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    if (request != null) {
                        // 同步请求头数据
                        String cookie = request.getHeader("Cookie");
                        // 给新请求同步Cookie
                        template.header("Cookie", cookie);
                    }
                }
            }
        };
    }
}
