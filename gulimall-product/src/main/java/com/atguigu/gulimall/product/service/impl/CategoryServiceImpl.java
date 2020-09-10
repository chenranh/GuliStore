package com.atguigu.gulimall.product.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.vo.forwebvo.Catelog2VO;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
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

    @Autowired
    StringRedisTemplate redisTemplate;



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
     *
     * @return
     */
    @Override
    public List<CategoryEntity> getLevel1Category() {
        List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));

        return categoryEntities;
    }

    /**
     * 查出所有分类，按要求进行组装
     * "1":[
     * {
     * "catalog1Id":"1",
     * "id":"1",
     * "name":"电子书刊",
     * "catalog3List":[
     * {
     * "catalog2Id":"1",
     * "id":"1",
     * "name":"电子书"
     * },
     * {
     * "catalog2Id":"1",
     * "id":"2",
     * "name":"网络原创"
     * },
     * {
     * "catalog2Id":"1",
     * "id":"3",
     * "name":"数字杂志"
     * },
     * {
     * "catalog2Id":"1",
     * "id":"4",
     * "name":"多媒体图书"
     * }
     * ],
     * },
     *
     * @return
     */


    //todo 堆外内存溢出
    //springboot2.0以后会默认使用lettuce作为操作redis客户端
    //lettuce的bug导致的堆外内存溢出
    //解决方案 1.升级lettuce客户端 2.切换使用jedis
    @Override
    public Map<String, List<Catelog2VO>> getCataLogJson() {
        //给缓存中放json字符串，拿出的json字符串需要逆转成能用的对象类型

        /**
         * 空结果缓存，解决缓存穿透
         * 设置过期时间，解决缓存雪崩
         * 加锁 解决缓存击穿
         */

        //加入缓存,缓存中存的是json字符串
        String catalogJson = redisTemplate.opsForValue().get("catalogJson");

        if (StrUtil.isEmpty(catalogJson)) {
            //缓存中没有
            System.out.println("*****************缓存不命中 查询数据库*******************");
            Map<String, List<Catelog2VO>> cataLogJsonFromDb = getCataLogJsonFromDbWithLocalLock();
            //查到的数据再放入缓存
            //todo 如果数据库没有查到这个数据，可以把它设为false或者不为null的值放入redis，解决缓存穿透的问题

            return cataLogJsonFromDb;
        }
        System.out.println("*****************缓存命中直接返回*******************");
        //转换成我们需要的类型,注意使用TypeReference （真好用！）
        Map<String, List<Catelog2VO>> result = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2VO>>>() {});

        return result;
    }


    //优化1 层级查询优化后的写法
    //查出所有分类，按要求进行组装
    //从数据库查询并封装数据
    //使用本地锁
    public Map<String, List<Catelog2VO>> getCataLogJsonFromDbWithLocalLock() {

        //只要是同一把锁，就能锁住整个需要锁的所有线程
        //本地锁只能锁住当前线程，分布式下需要分布式锁
        //1.synchronized (this) springboot中所有的组件在容器中都是单例的
        synchronized (this) {
            //得到锁以后应该去缓存中再去确认一次，如果没有才需要继续查询
            String catalogJson = redisTemplate.opsForValue().get("catalogJson");
            if (StrUtil.isNotEmpty(catalogJson)) {
                //如果缓存不为空直接返回
                Map<String, List<Catelog2VO>> result = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2VO>>>() {
                });
                return result;
            }
            System.out.println("*****************************查询了数据库************************************");

            /**
             * 1.将数据库的多次查询变为一次
             */
            List<CategoryEntity> selectList = baseMapper.selectList(null);


            //查出所有1级分类
            List<CategoryEntity> level1List = getParent_cid(selectList, 0L);
            //封装数据,K是一级分类前面的数字
            Map<String, List<Catelog2VO>> parent_cid = level1List.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
                //每一个的1级分类，查到这个1级分类的2级分类，这里把循环查数据库优化
                List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
                //封装上面的结果
                List<Catelog2VO> catelog2Vos = null;
                if (categoryEntities != null) {
                    catelog2Vos = categoryEntities.stream().map(level2 -> {
                        Catelog2VO catelog2Vo = new Catelog2VO(v.getCatId().toString(), null, level2.getCatId().toString(), level2.getName());
                        //找当前二级分类找三级分类封装成vo，这里把循环查数据库优化
                        List<CategoryEntity> level3Catelog = getParent_cid(selectList, level2.getCatId());
                        if (level3Catelog != null) {
                            List<Catelog2VO.Catelog3VO> level3List = level3Catelog.stream().map(level3 -> {
                                //封装成指定格式
                                Catelog2VO.Catelog3VO catelog3VO = new Catelog2VO.Catelog3VO(level2.getCatId().toString(), level3.getCatId().toString(), level3.getName());
                                return catelog3VO;
                            }).collect(Collectors.toList());
                            catelog2Vo.setCatalog3List(level3List);
                        }
                        return catelog2Vo;
                    }).collect(Collectors.toList());
                }
                return catelog2Vos;
            }));
            //查到的数据再放入到缓存，将对象转成json放在缓存中
            String s = JSON.toJSONString(parent_cid);
            redisTemplate.opsForValue().set("catalogJSON", s, 1, TimeUnit.DAYS);
            return parent_cid;
        }


    }


    //使用分布式锁  核心加锁保证原子性 解锁保证原子性
    public Map<String, List<Catelog2VO>> getCataLogJsonFromDbWithRedisLock() {

        //占分布式锁。同时设置过期时间 去redis占坑
        String uuid = UUID.randomUUID().toString();
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);

        if (lock) {
            //加锁成功 执行业务
            //设置过期时间 防止死锁必须和加锁是同一条命令 原子性
            //redisTemplate.expire("lock",30,TimeUnit.SECONDS);
            Map<String, List<Catelog2VO>> dataFromDb;
            try {
                dataFromDb = getDataFromDb();
            } finally {
                //获取值成功+对比成功删除是原子性  lua脚本解锁
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                //删除锁
                Long lock1 = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList("lock"), uuid);
            }
            //执行完以后删除锁 ，存在业务还没走完锁过期，别的线程抢占锁，这时候删除锁删除的是别人的锁，改进是增加uuid删除自己的锁
            //redisTemplate.delete("lock");
            //根据uuid取值拿到自己的锁进行删锁
//            String lockValue = redisTemplate.opsForValue().get("lock");
//            if (uuid.equals(lockValue)) {
//                //删除自己的锁，存在问题是在在判断完是自己的锁，在redis传给服务端的过程中锁过期，别的线程抢占锁。这个时候删除的锁还是别人的锁
//                redisTemplate.delete("lock");
//            }
            return dataFromDb;
        } else {
            //加锁失败 重试自旋的方式 自己调用自己  相当于自旋锁！！！！！？？？
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return getCataLogJsonFromDbWithRedisLock();
        }


    }

    private Map<String, List<Catelog2VO>> getDataFromDb() {
        String catalogJson = redisTemplate.opsForValue().get("catalogJson");
        if (StrUtil.isNotEmpty(catalogJson)) {
            //如果缓存不为空直接返回
            Map<String, List<Catelog2VO>> result = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2VO>>>() {
            });
            return result;
        }

        /**
         * 1.将数据库的多次查询变为一次
         */
        List<CategoryEntity> selectList = baseMapper.selectList(null);


        //查出所有1级分类
        List<CategoryEntity> level1List = getParent_cid(selectList, 0L);
        //封装数据,K是一级分类前面的数字
        Map<String, List<Catelog2VO>> parent_cid = level1List.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //每一个的1级分类，查到这个1级分类的2级分类，这里把循环查数据库优化
            List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
            //封装上面的结果
            List<Catelog2VO> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map(level2 -> {
                    Catelog2VO catelog2Vo = new Catelog2VO(v.getCatId().toString(), null, level2.getCatId().toString(), level2.getName());
                    //找当前二级分类找三级分类封装成vo，这里把循环查数据库优化
                    List<CategoryEntity> level3Catelog = getParent_cid(selectList, level2.getCatId());
                    if (level3Catelog != null) {
                        List<Catelog2VO.Catelog3VO> level3List = level3Catelog.stream().map(level3 -> {
                            //封装成指定格式
                            Catelog2VO.Catelog3VO catelog3VO = new Catelog2VO.Catelog3VO(level2.getCatId().toString(), level3.getCatId().toString(), level3.getName());
                            return catelog3VO;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(level3List);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));
        //查到的数据再放入到缓存，将对象转成json放在缓存中
        String s = JSON.toJSONString(parent_cid);
        redisTemplate.opsForValue().set("catalogJSON", s, 1, TimeUnit.DAYS);
        return parent_cid;
    }


    //抽取出来的方法，根据父id过滤对象 refactor/extract/method
    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList, Long parent_cid) {
        //找到parent_cid是指定的
        List<CategoryEntity> collect = selectList.stream().filter(item -> item.getParentCid().equals(parent_cid)).collect(Collectors.toList());
        //return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", parent_cid));
        return collect;
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
