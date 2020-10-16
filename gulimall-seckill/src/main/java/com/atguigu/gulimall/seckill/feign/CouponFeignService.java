package com.atguigu.gulimall.seckill.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * <p>Title: CouponFeignService</p>
 * Description：
 * date：2020/7/6 17:35
 */
@FeignClient("gulimall-coupon")
public interface CouponFeignService {

	@GetMapping("/coupon/seckillsession/lates3DaySession")
	R getLate3DaySession();
}
