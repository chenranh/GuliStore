package com.atguigu.gulimallcart.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.gulimallcart.feign.ProductFeignService;
import com.atguigu.gulimallcart.interceptor.CartInterceptor;
import com.atguigu.gulimallcart.service.CartService;
import com.atguigu.gulimallcart.vo.Cart;
import com.atguigu.gulimallcart.vo.CartItem;
import com.atguigu.gulimallcart.vo.SkuInfoVo;
import com.atguigu.gulimallcart.vo.UserInfoTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    ThreadPoolExecutor executor;

    private final String CART_PREFIX = "gulimall:cart:";

    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        String res = (String) cartOps.get(skuId.toString());

        if (StrUtil.isEmpty(res)) {
            //购物车中无此商品 添加新商品到购物车
            CartItem cartItem = new CartItem();
            //1.远程查询当前要添加的商品的信息
            CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
                R skuInfo = productFeignService.getSkuInfo(skuId);
                SkuInfoVo data = skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                });

                cartItem.setCount(num);
                cartItem.setCheck(true);
                cartItem.setImage(data.getSkuDefaultImg());
                cartItem.setPrice(data.getPrice());
                cartItem.setTitle(data.getSkuTitle());
                cartItem.setSkuId(skuId);
            }, executor);

            //2.远程查询sku的组合信息
            CompletableFuture<Void> getSkuSaleAttrValues = CompletableFuture.runAsync(() -> {
                List<String> values = productFeignService.getSkuSaleAttrValues(skuId);
                cartItem.setSkuAttr(values);
            }, executor);

            //等异步线程都执行完再给redis中放数据 get()是阻塞式等待
            CompletableFuture.allOf(getSkuSaleAttrValues, getSkuInfoTask).get();

            String s = JSON.toJSONString(cartItem);
            cartOps.put(skuId.toString(), s);
            return cartItem;
        } else {
            //购物车有此商品，修改数量
            CartItem cartItem = JSON.parseObject(res, CartItem.class);
            cartItem.setCount(cartItem.getCount() + num);
            cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
            return cartItem;
        }

    }

    @Override
    public CartItem getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String str = (String) cartOps.get(skuId.toString());
        CartItem cartItem = JSON.parseObject(str, CartItem.class);
        return cartItem;
    }

    @Override
    public Cart getCart() throws ExecutionException, InterruptedException {
        Cart cart = new Cart();
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if (userInfoTo.getUserId() != null) {
            //1.登录状态
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            //如果临时购物车的数据还没有进行合并
            String tempCart = CART_PREFIX + userInfoTo.getUserKey();
            List<CartItem> tempItems = getCartItems(tempCart);
            if (CollectionUtil.isEmpty(tempItems)){
                //临时购物车里面有商品，需要合并
                for (CartItem tempItem : tempItems) {
                    addToCart(tempItem.getSkuId(),tempItem.getCount());
                }
                //清空临时购物车
                clearCart(tempCart);
            }
            //获取登陆后的购物车的数据（包括合并过来的临时购物车数据，和登陆后的购物车的数据）
            List<CartItem> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);

        } else {
            //2.没有登录
            String cartKey = CART_PREFIX + userInfoTo.getUserKey();
            //获取临时购物车的所有 购物项
            List<CartItem> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);
        }
        return cart;
    }


    /**
     * 获取购物车所有项
     */
    private List<CartItem> getCartItems(String cartKey){
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(cartKey);
        List<Object> values = hashOps.values();
        if(values != null && values.size() > 0){
            List<CartItem> collect = values.stream().map(value -> {
                String str = (String) value;
                CartItem cartItem = JSON.parseObject(str, CartItem.class);
                return cartItem;
            }).collect(Collectors.toList());
            return collect;
        }
        return null;
    }


    /**
     * 获取到我们要操作的购物车
     *
     * @return
     */
    private BoundHashOperations<String, Object, Object> getCartOps() {
        //1.得到用户信息
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        //根据用户是否登录选择存入redis不同的key
        String cartKey = "";
        if (userInfoTo.getUserId() != null) {
            cartKey = CART_PREFIX + userInfoTo.getUserId();
        } else {
            cartKey = CART_PREFIX + userInfoTo.getUserKey();
        }
        BoundHashOperations<String, Object, Object> cartOps = redisTemplate.boundHashOps(cartKey);
        //绑定购物车的key
        return cartOps;
    }

    @Override
    public void clearCart(String cartKey){
        redisTemplate.delete(cartKey);
    }

    @Override
    public void checkItem(Long skuId, Integer check) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCheck(check==1?true:false);
        String s = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(),s);
    }

    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }

    @Override
    public void changeItemCount(Long skuId, Integer num) {
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCount(num);
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
    }


}
