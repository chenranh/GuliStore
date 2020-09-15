package com.atguigu.gulimall.search.vo;

import lombok.Data;

import java.util.List;

/**
 * 封装页面所有可能传递过来的查询条件
 * catelogid=225&keyword=小米&sort=saleCount_asc&hasStock=0/1&brandId=1&brandId=2
 * &attrs=1_5寸：8寸&attrs=2_16G:8G
 */
@Data
public class SearchParm {
    private String keyWord;  //全文匹配关键字

    private Long catelog3Id;  //分类id

    /**
     * sort=saleCount_asc
     * sort=skuPrice_asc
     * sort=hotScore_asc
     */
    private String sort;  //排序条件

    /**
     * 过滤条件
     * hasStock(是否有货)、skuPrice、brandId、catelog3Id,attrs
     * hasStock=0/1
     * skuPrice=1_50/_500/500_
     * brandId=1
     * attrs=2_5寸：6寸
     */
    private Integer hasStock=1;  //是否有货  0无库存 1有库存
    private String skuPrice;  //价格区间
    private List<Long> brandId;  //按照品牌id进行筛选

    private List<String> attrs; //按照属性进行筛选

     private Integer pageNum=1; //页码
}
