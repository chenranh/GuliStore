package com.atguigu.gulimallcart.service;

import com.atguigu.gulimallcart.vo.Cart;
import com.atguigu.gulimallcart.vo.CartItem;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface CartService {
    /**
     * 将商品添加到购物车
     * @param skuId
     * @param num
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    /**
     * 获取购物车中的某个购物项
     * @param skuId
     * @return
     */
    CartItem getCartItem(Long skuId);

    /**
     * 获取整个购物车
     * @return
     */
    Cart getCart() throws ExecutionException, InterruptedException;


    /**
     * 清空购物车数据
     * @param cartKey
     */
    void clearCart(String cartKey);

    /**
     * 勾选购物项
     * @param skuId
     * @param check
     */
    void checkItem(Long skuId, Integer check);

    /**
     * 删除购物项
     * @param skuId
     */
    void deleteItem(Long skuId);

    /**
     * 改变商品的数量
     * @param skuId
     * @param num
     */
    void changeItemCount(Long skuId, Integer num);

    /**
     * 获取当前登录用户购物车里面的数据
     * @return
     */
    List<CartItem> getUserCartItems();

}
