package com.atguigu.gulimall.product.service;

import com.atguigu.gulimall.product.vo.forwebvo.Catelog2VO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.CategoryEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品三级分类
 *
 * @author leifengyang
 * @email leifengyang@gmail.com
 * @date 2019-10-01 21:08:48
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<CategoryEntity> listWithTree();

    void removeMenuByIds(List<Long> asList);


    /**
     * 找到catelogId的完整路径；
     * [父/子/孙]
     * @param catelogId
     * @return
     */
    Long[] findCatelogPath(Long catelogId);

    void updateCascade(CategoryEntity category);

    /**
     * 查询所有一级分类给web首页显示
     * @return
     */
    List<CategoryEntity> getLevel1Category();

    /**
     * 查出所有分类，按要求进行组装
     * @return
     */
    Map<String, List<Catelog2VO>> getCataLogJson();
}

