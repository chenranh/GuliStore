package com.atguigu.gulimallcart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.gulimallcart.feign.ProductFeignService;
import com.atguigu.gulimallcart.interceptor.CartInterceptor;
import com.atguigu.gulimallcart.service.CartService;
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

@Slf4j
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    ThreadPoolExecutor executor;

    private final String CART_PREFIX="gulimall:cart:";

    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        //  添加商品到购物车
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
        CompletableFuture.allOf(getSkuSaleAttrValues,getSkuInfoTask).get();

        String s = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(),s);
        return cartItem;
    }

    /**
     * 获取到我们要操作的购物车
     * @return
     */
    private BoundHashOperations<String, Object, Object> getCartOps() {
        //1.得到用户信息
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        //根据用户是否登录选择存入redis不同的key
        String cartKey="";
        if (userInfoTo.getUserId()!=null){
            cartKey=CART_PREFIX+userInfoTo.getUserId();
        }else {
            cartKey=CART_PREFIX+userInfoTo.getUserKey();
        }
        BoundHashOperations<String, Object, Object> cartOps = redisTemplate.boundHashOps(cartKey);
        //绑定购物车的key
        return cartOps;
    }
}
