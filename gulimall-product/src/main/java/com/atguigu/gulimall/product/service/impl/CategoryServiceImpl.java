package com.atguigu.gulimall.product.service.impl;

import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.vo.forwebvo.Catelog2VO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

//    @Autowired
//    CategoryDao categoryDao;

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //1、查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);

        //2、组装成父子的树形结构

        //2.1）、找到所有的一级分类
        List<CategoryEntity> level1Menus = entities.stream().filter(categoryEntity ->
                categoryEntity.getParentCid() == 0
        ).map((menu) -> {
            menu.setChildren(getChildrens(menu, entities));
            return menu;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());

        return level1Menus;
    }

    //递归查找所有菜单的子菜单
    private List<CategoryEntity> getChildrens(CategoryEntity root, List<CategoryEntity> all) {

        List<CategoryEntity> children = all.stream()
                .filter(categoryEntity -> categoryEntity.getParentCid() == root.getCatId())
                .map(categoryEntity -> {
                    //1、找到子菜单
                    categoryEntity.setChildren(getChildrens(categoryEntity, all));
                    return categoryEntity;
                })
                .sorted(Comparator.comparingInt(menu -> (menu.getSort() == null ? 0 : menu.getSort())))
                .collect(Collectors.toList());

        return children;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO  1、检查当前删除的菜单，是否被别的地方引用

        //逻辑删除
        baseMapper.deleteBatchIds(asList);
    }

    //[2,25,225]
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);

        Collections.reverse(parentPath);


        return parentPath.toArray(new Long[parentPath.size()]);
    }

    /**
     * 级联更新所有关联的数据
     *
     * @param category
     */
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }

    /**
     * 查询所有一级分类，web页面显示
     * @return
     */
    @Override
    public List<CategoryEntity> getLevel1Category() {
        List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));

        return categoryEntities;
    }

    /**
     * 查出所有分类，按要求进行组装
     *  "1":[
     *         {
     *             "catalog1Id":"1",
     *             "id":"1",
     *             "name":"电子书刊",
     *             "catalog3List":[
     *                 {
     *                     "catalog2Id":"1",
     *                     "id":"1",
     *                     "name":"电子书"
     *                 },
     *                 {
     *                     "catalog2Id":"1",
     *                     "id":"2",
     *                     "name":"网络原创"
     *                 },
     *                 {
     *                     "catalog2Id":"1",
     *                     "id":"3",
     *                     "name":"数字杂志"
     *                 },
     *                 {
     *                     "catalog2Id":"1",
     *                     "id":"4",
     *                     "name":"多媒体图书"
     *                 }
     *             ],
     *         },
     * @return
     */

    //层级查询优化后的写法
    //查出所有分类，按要求进行组装
    @Override
    public Map<String, List<Catelog2VO>> getCataLogJson() {
        /**
         * 1.将数据库的多次查询变为一次
         */
        List<CategoryEntity> selectList = baseMapper.selectList(null);


        //查出所有1级分类
        List<CategoryEntity> level1List = getParent_cid(selectList,0L);
        //封装数据,K是一级分类前面的数字
        Map<String, List<Catelog2VO>> parent_cid = level1List.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //每一个的1级分类，查到这个1级分类的2级分类，这里把循环查数据库优化
            List<CategoryEntity> categoryEntities = getParent_cid(selectList,v.getCatId());
            //封装上面的结果
            List<Catelog2VO> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map(level2 -> {
                    Catelog2VO catelog2Vo = new Catelog2VO(v.getCatId().toString(),null , level2.getCatId().toString(), level2.getName());
                    //找当前二级分类找三级分类封装成vo，这里把循环查数据库优化
                    List<CategoryEntity> level3Catelog = getParent_cid(selectList,level2.getCatId());
                    if (level3Catelog!=null){
                        List<Catelog2VO.Catelog3VO> level3List = level3Catelog.stream().map(level3 -> {
                            //封装成指定格式
                            Catelog2VO.Catelog3VO catelog3VO = new Catelog2VO.Catelog3VO(level2.getCatId().toString() ,level3.getCatId().toString(),level3.getName());
                            return catelog3VO;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(level3List);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));
        return parent_cid;
    }

    //抽取出来的方法 refactor/extract/method
    private List<CategoryEntity> getParent_cid( List<CategoryEntity> selectList,Long parent_cid) {
        //找到parent_cid是指定的
        selectList.stream().filter(item->item.getParentCid()==parent_cid).collect(Collectors.toList());
        //return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", parent_cid));
        return selectList;
    }

//    @Override
//    public Map<String, List<Catelog2VO>> getCataLogJson() {
//        //查出所有1级分类
//        List<CategoryEntity> level1List = getLevel1Category();
//        //封装数据,K是一级分类前面的数字
//        Map<String, List<Catelog2VO>> parent_cid = level1List.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
//            //每一个的1级分类，查到这个1级分类的2级分类
//            List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", v.getCatId()));
//            //封装上面的结果
//            List<Catelog2VO> catelog2Vos = null;
//            if (categoryEntities != null) {
//                catelog2Vos = categoryEntities.stream().map(level2 -> {
//                    Catelog2VO catelog2Vo = new Catelog2VO(v.getCatId().toString(),null , level2.getCatId().toString(), level2.getName());
//                    //找当前二级分类找三级分类封装成vo
//                    List<CategoryEntity> level3Catelog = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", level2.getCatId()));
//                    if (level3Catelog!=null){
//                        List<Catelog2VO.Catelog3VO> level3List = level3Catelog.stream().map(level3 -> {
//                            //封装成指定格式
//                            Catelog2VO.Catelog3VO catelog3VO = new Catelog2VO.Catelog3VO(level2.getCatId().toString() ,level3.getCatId().toString(),level3.getName());
//                            return catelog3VO;
//                        }).collect(Collectors.toList());
//                        catelog2Vo.setCatalog3List(level3List);
//                    }
//                    return catelog2Vo;
//                }).collect(Collectors.toList());
//            }
//            return catelog2Vos;
//        }));
//        return parent_cid;
//    }

    //225,25,2
    private List<Long> findParentPath(Long catelogId, List<Long> paths) {
        //1、收集当前节点id
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if (byId.getParentCid() != 0) {
            findParentPath(byId.getParentCid(), paths);
        }
        return paths;

    }


}
