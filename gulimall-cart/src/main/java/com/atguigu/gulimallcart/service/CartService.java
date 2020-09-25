package com.atguigu.gulimallcart.service;

import com.atguigu.gulimallcart.vo.CartItem;

import java.util.concurrent.ExecutionException;

public interface CartService {
    CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;
}
