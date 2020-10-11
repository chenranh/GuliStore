package com.atguigu.gulimall.order.feign;

import com.atguigu.common.utils.R;
import com.atguigu.gulimall.order.vo.WareSkuLockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * <p>Title: WmsFeignService</p>
 * Description：
 * date：2020/7/1 11:58
 */
@FeignClient("gulimall-ware")
public interface WmsFeignService {

	/**
	 * 检查sku是否有库存
	 * @param SkuIds
	 * @return
	 */
	@PostMapping("/ware/waresku/hasstock")
	R getSkuHasStock(@RequestBody List<Long> SkuIds);


	@GetMapping("/ware/wareinfo/fare")
	R getFare(@RequestParam("addrId") Long addrId);

	/**
	 * 锁定库存
	 */
	@PostMapping("/ware/waresku/lock/order")
	R orderLockStock(@RequestBody WareSkuLockVo vo);
}
