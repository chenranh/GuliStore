package com.atguigu.gulimall.search.vo;

import com.atguigu.common.to.es.SkuEsModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SearchResult {

    private List<SkuEsModel> products; //查询到的商品信息

    private Long total;  //总记录数

    private Integer pageNum;  //当前页码

    private Integer totalPages; //总页码

    private List<Integer> pageNavs; //导航页码

    public List<BrandVo> brands; //当前查询的结果，所有涉及到的品牌
    public List<CatelogVo> catelogs;  //
    public List<AttrVo> attrs;  //当前查询的结果  所有涉及到的所有属性

    //============================以上是返给页面的信息===========================

    // 面包屑导航数据
    private  List<NavVo> navs = new ArrayList<>();

    /**
     * 便于判断当前id是否被使用
     */
    private List<Long> attrIds = new ArrayList<>();

    @Data
    public static class NavVo{
        private String name;

        private String navValue;

        private String link;
    }

    @Data
    public static class BrandVo {
        private Long brandId;
        private String brandName;
        private String brandImg;
    }

    @Data
    public static class AttrVo {

        private Long attrId;
        private String attrName;
        private List<String> attrValue;
    }

    @Data
    public static class CatelogVo {

        private Long catelogId;

        private String catelogName;
    }

}
