package com.atguigu.gulimall.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.fastjson.JSON;
import com.atguigu.common.exception.BizCodeEnume;
import com.atguigu.common.utils.R;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * <p>Title: SentinelGateWayConfig</p>
 * Description：
 */
@Configuration
public class SentinelGateWayConfig {
	// TODO: 2020/10/22 响应式编程 webflux
	public SentinelGateWayConfig(){
		GatewayCallbackManager.setBlockHandler((exchange, t) ->{
			// 网关限流了请求 就会回调这个方法
			R error = R.error(BizCodeEnume.TO_MANY_REQUEST.getCode(), BizCodeEnume.TO_MANY_REQUEST.getMsg());
			String errJson = JSON.toJSONString(error);
			Mono<ServerResponse> body = ServerResponse.ok().body(Mono.just(errJson), String.class);
			return body;
		});
	}
}
