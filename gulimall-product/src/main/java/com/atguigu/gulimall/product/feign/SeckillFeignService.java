package com.atguigu.gulimall.product.feign;

import com.atguigu.common.utils.R;
//import com.atguigu.gulimall.product.fallback.SeckillFeignServiceFallBack;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * <p>Title: SeckillFeignService</p>
 * Description：
 * date：2020/7/9 12:28
 */

//fallback表示，远程接口出问题以后，调用本地接口实现，返回错误码和错误信息
//@FeignClient(value = "gulimall-seckill",fallback = SeckillFeignServiceFallBack.class)
@FeignClient(value = "gulimall-seckill")
public interface SeckillFeignService {

	@GetMapping("/sku/seckill/{skuId}")
	R getSkuSeckillInfo(@PathVariable("skuId") Long skuId);
}
