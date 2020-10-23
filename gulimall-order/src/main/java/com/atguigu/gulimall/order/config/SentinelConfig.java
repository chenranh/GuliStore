//package com.atguigu.gulimall.order.config;
//
//import com.alibaba.csp.sentinel.adapter.servlet.callback.WebCallbackManager;
//import com.alibaba.fastjson.JSON;
//import com.atguigu.common.exception.BizCodeEnume;
//import com.atguigu.common.utils.R;
//import org.springframework.context.annotation.Configuration;
//
///**
// * <p>Title: SecKillSentinelConfig</p>
// * Description：配置请求被限制以后的处理器
// */
//@Configuration
//public class SentinelConfig {
//
//	public SentinelConfig(){
//		WebCallbackManager.setUrlBlockHandler((request, response, exception) -> {
//			R error = R.error(BizCodeEnume.TO_MANY_REQUEST.getCode(), BizCodeEnume.TO_MANY_REQUEST.getMsg());
//			response.setCharacterEncoding("UTF-8");
//			response.setContentType("application/json");
//			response.getWriter().write(JSON.toJSONString(error));
//		});
//	}
//}
