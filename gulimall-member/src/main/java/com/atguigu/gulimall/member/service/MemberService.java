package com.atguigu.gulimall.member.service;

import com.atguigu.gulimall.exception.PhoneExsitException;
import com.atguigu.gulimall.exception.UserNameExistException;
import com.atguigu.gulimall.vo.MemberRegistVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.member.entity.MemberEntity;

import java.util.Map;

/**
 *
 *
 * @author yuke
 * @email 627617510@gmail.com
 * @date 2020-08-23 17:46:43
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void regist(MemberRegistVo vo);

    void checkPhoneUnique(String phone) throws PhoneExsitException;

    void checkUsernameUnique(String userName) throws UserNameExistException;
}

