package com.atguigu.gulimall.order.service.impl;

import com.atguigu.common.vo.MemberRsepVo;
import com.atguigu.gulimall.order.feign.CartFeignService;
import com.atguigu.gulimall.order.feign.MembenFeignService;
import com.atguigu.gulimall.order.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.order.vo.MemberAddressVo;
import com.atguigu.gulimall.order.vo.OrderConfirmVo;
import com.atguigu.gulimall.order.vo.OrderItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.order.dao.OrderDao;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.service.OrderService;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {


    @Autowired
    MembenFeignService membenFeignService;

    @Autowired
    CartFeignService cartFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        //注意这里对 threadLocal的应用
        MemberRsepVo memberRsepVo = LoginUserInterceptor.threadLocal.get();
        //1.远程查询所有的地址列表
        List<MemberAddressVo> address = membenFeignService.getAddress(memberRsepVo.getId());
        confirmVo.setAddress(address);
        //2.远程查询购物车所有选中的购物项
        List<OrderItemVo> items = cartFeignService.getCurrentUserCartItems();
        confirmVo.setItems(items);
        //3.查询用户积分
        Integer integration = memberRsepVo.getIntegration();
        confirmVo.setIntegration(integration);
        //其他数据根据items自动计算 订单的总额

        return confirmVo;
    }

}