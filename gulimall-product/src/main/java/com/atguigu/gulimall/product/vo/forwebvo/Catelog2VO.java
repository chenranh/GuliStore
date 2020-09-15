package com.atguigu.gulimall.product.vo.forwebvo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @AUTHOR: raymond
 * @DATETIME: 2020/5/14  20:58
 * DESCRIPTION:
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Catelog2VO {

    private String catelogId;//1级分类id

    private List<Catelog3VO> catelog3List;//三级子分类

    private String id;

    private String name;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Catelog3VO {

        private String catelog2Id;//父分类 2级分类id

        private String id;

        private String name;

    }

}
