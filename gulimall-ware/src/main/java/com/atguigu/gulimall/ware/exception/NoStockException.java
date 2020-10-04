package com.atguigu.gulimall.ware.exception;


/**
 * @title: NoStockException
 * @Author yuke
 * @Date: 2020-10-04 9:45
 */
public class NoStockException extends RuntimeException {


    private Long skuId;

    public NoStockException(Long skuId) {
        super("商品id:" + skuId + "; 没有足够的库存了");
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }
}
