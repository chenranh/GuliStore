package com.atguigu.gulimall.order.dao;

import com.atguigu.gulimall.order.entity.MqMessageEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * (MqMessage)表数据库访问层
 *
 * @author @孙启新
 * @since 2020-08-13 13:57:23
 */
@Mapper
public interface MqMessageDao extends BaseMapper<MqMessageEntity> {

}
