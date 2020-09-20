package com.atguigu.gulimall.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * <p>Title: MyWebConfig</p>
 * Description：页面映射
 */
@Configuration
public class MyWebConfig implements WebMvcConfigurer {


	/**
	 * 视图映射
	 * @param registry
	 */
	@Override
	public void addViewControllers(ViewControllerRegistry registry) {

		/**
		 * @GetMapping("/reg.html")
		 *     public String regPage() {
		 *         return "reg";
		 *     }
		 */
		registry.addViewController("/reg.html").setViewName("reg");
		registry.addViewController("/login.html").setViewName("login");
	}
}
