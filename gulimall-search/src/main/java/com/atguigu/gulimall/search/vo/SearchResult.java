package com.atguigu.gulimall.search.vo;

import com.atguigu.common.to.es.SkuEsModel;
import lombok.Data;

import java.util.List;

@Data
public class SearchResult {

    private List<SkuEsModel> products; //查询到的商品信息

    private Integer total;  //总记录数

    private Integer pageNum;  //当前页码

    private Integer totalPages; //总页码

    private List<BrandVo> brands; //当前查询的结果，所有涉及到的品牌
    private List<CatelogVo> catelogs;  //
    private List<AttrVo> attrs;  //当前查询的结果  所有涉及到的所有属性

    //============================以上是返给页面的信息===========================
    @Data
    private static class BrandVo {
        private Long brandId;
        private String brandName;
        private Long brandImg;
    }

    @Data
    private static class AttrVo {

        private Long attrId;
        private String attrName;
        private List<String> attrValue;
    }

    @Data
    private static class CatelogVo {

        private Long catelogId;

        private String catelogName;
    }

}
