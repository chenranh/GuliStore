/**
  * Copyright 2019 bejson.com
  */
package com.atguigu.gulimall.product.vo.productaddvo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 会员相关
 * 购买积分和成长积分
 */
@Data
public class Bounds {

    private BigDecimal buyBounds;
    private BigDecimal growBounds;


}
