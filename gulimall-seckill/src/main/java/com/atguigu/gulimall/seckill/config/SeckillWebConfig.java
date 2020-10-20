package com.atguigu.gulimall.seckill.config;

import com.atguigu.gulimall.seckill.interceptor.LoginUserInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * <p>Title: SeckillWebConfig</p>
 * Description：
 * date：2020/7/9 16:02
 */
@Configuration
public class SeckillWebConfig implements WebMvcConfigurer {

	@Autowired
	private LoginUserInterceptor loginUserInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		//当前项目的所有请求都要用这个拦截器拦截
		registry.addInterceptor(loginUserInterceptor).addPathPatterns("/**");
	}
}
