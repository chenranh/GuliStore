package com.atguigu.gulimallcart.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物项类型
 */
@Data
public class CartItem {
    private Long skuId;
    private Boolean check = true;
    private String title;
    private String image;
    private List<String> skuAttr;
    private BigDecimal price;
    private BigDecimal totalPrice;
    private Integer count;

    /**
     * 计算购物项的总价
     * @return
     */
    public BigDecimal getTotalPrice() {
        BigDecimal totalPrice = this.price.multiply(new BigDecimal("" + this.count));
        return totalPrice;
    }
}
