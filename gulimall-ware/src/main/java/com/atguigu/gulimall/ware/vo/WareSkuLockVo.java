package com.atguigu.gulimall.ware.vo;

import lombok.Data;

import java.util.List;

/**
 * 锁库存需要的对象
 */
@Data
public class WareSkuLockVo {

	/**
	 * 订单号
	 */
	private String orderSn;

	/**
	 * 要锁住的所有库存信息
	 */
	private List<OrderItemVo> locks;

}
