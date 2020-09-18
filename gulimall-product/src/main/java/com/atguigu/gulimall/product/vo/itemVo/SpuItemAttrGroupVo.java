package com.atguigu.gulimall.product.vo.itemVo;

import com.atguigu.gulimall.product.vo.productaddvo.Attr;
import lombok.Data;

import java.util.List;

/**
 * @title: SpuItemAttrGroupVo
 * @Author yuke
 * @Date: 2020-09-18 11:21
 */
@Data
public class SpuItemAttrGroupVo {

        private String groupName;
        private List<Attr> attrs;

}
