package com.atguigu.gulimall.order.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Description：订单确认页需要的数据
 */
@ToString
@Data
public class OrderConfirmVo {

    /**
     * 收获地址
     */
    List<MemberAddressVo> address;

    /**
     * 所有选中的购物项
     */
    List<OrderItemVo> items;

    /**
     * 积分信息
     */
    private Integer integration;


    /**
     * 防重令牌
     */
    private String orderToken;

    /**
     * 用于判断某个sku是否有库存
     */
    Map<Long, Boolean> stocks;


    /**
     * 订单总额
     */
    BigDecimal total;

    public BigDecimal getTotal() {
        BigDecimal sum = new BigDecimal("0");
        if (items != null) {
            for (OrderItemVo item : items) {
                sum = sum.add(item.getPrice().multiply(new BigDecimal(item.getCount().toString())));
            }
        }
        return sum;
    }

    /**
     * 应付价格
     */
    BigDecimal payPrice;

    public BigDecimal getPayPrice() {
        return getTotal();
    }

    /**
     * // TODO: 2020-09-30 总件数是一种商品的总件数还是所有商品的总件数
     * 是一种商品的总件数
     * 商品总件数
     * @return
     */
    Integer count;

    public Integer getCount() {
        Integer i = 0;
        if (items != null) {
            for (OrderItemVo item : items) {
                i += item.getCount();
            }
        }
        return i;
    }
}
