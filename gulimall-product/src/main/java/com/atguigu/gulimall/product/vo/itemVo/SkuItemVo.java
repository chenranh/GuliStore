package com.atguigu.gulimall.product.vo.itemVo;

import com.atguigu.gulimall.product.entity.SkuImagesEntity;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;
import com.atguigu.gulimall.product.vo.SeckillInfoVo;
import lombok.Data;

import java.util.List;

/**
 * @title: SkuItemVO
 * @Author yuke
 * @Date: 2020-09-18
 */
@Data
public class SkuItemVo {
    //1.sku基本信息获取 pms_sku_info
    SkuInfoEntity info;

    boolean hasStock=true;

    //2.sku的图片信息 pms_sku_images
    private List<SkuImagesEntity> images;

    //3.spu的销售属性组合
    List<SkuItemSaleAttrVo> saleAttr;

    //4.spu的介绍
    private SpuInfoDescEntity desp;

    //5.spu的规格参数信息
    List<SpuItemAttrGroupVo> groupAttrs;

    //当前商品的秒杀优惠信息
    SeckillInfoVo seckillInfoVo;

}
