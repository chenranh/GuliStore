package com.atguigu.gulimall.ware.vo;

import lombok.Data;

/**
 * 锁库存对象
 */
@Data
public class LockStockResult {
	private Long skuId;

	private Integer num;

	private Boolean locked;

}
