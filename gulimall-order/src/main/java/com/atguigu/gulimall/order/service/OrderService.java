package com.atguigu.gulimall.order.service;

import com.atguigu.gulimall.order.vo.OrderConfirmVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.order.entity.OrderEntity;

import java.util.Map;

/**
 * ????
 *
 * @author yuke
 * @email 627617510@gmail.com
 * @date 2020-08-23 18:18:25
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 订单确认页返回需要用的数据
     * @return
     */
    OrderConfirmVo confirmOrder();
}

