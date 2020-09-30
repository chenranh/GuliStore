package com.atguigu.gulimall.ware.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * <p>Title: FareVo</p>
 * Description：用于前端页面显示优惠价格和用户信息
 */
@Data
public class FareVo {

	private MemberAddressVo memberAddressVo;

	private BigDecimal fare;
}
