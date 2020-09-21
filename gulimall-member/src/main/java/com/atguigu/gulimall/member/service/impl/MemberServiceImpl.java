package com.atguigu.gulimall.member.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.atguigu.gulimall.exception.PhoneExsitException;
import com.atguigu.gulimall.exception.UserNameExistException;
import com.atguigu.gulimall.member.dao.MemberLevelDao;
import com.atguigu.gulimall.member.entity.MemberLevelEntity;
import com.atguigu.gulimall.vo.MemberRegistVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.member.dao.MemberDao;
import com.atguigu.gulimall.member.entity.MemberEntity;
import com.atguigu.gulimall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 注册会员
     *
     * @param vo
     */
    @Override
    public void regist(MemberRegistVo vo) {
        MemberEntity entity = new MemberEntity();
        //设置默认等级
        MemberLevelEntity levelEntity = memberLevelDao.getDefaultLevel();
        entity.setLevelId(levelEntity.getId());
        //检查用户名和手机号是否唯一  为了让controller能感知异常 异常机制
        checkPhoneUnique(vo.getPhone());
        checkUsernameUnique(vo.getUserName());

        entity.setMobile(vo.getPhone());
        entity.setUsername(vo.getUserName());
        baseMapper.insert(entity);
    }

    @Override
    public void checkPhoneUnique(String phone) throws PhoneExsitException {
        MemberDao memberDao = this.baseMapper;
        Integer mobile = memberDao.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if (mobile > 0) {
            throw new PhoneExsitException();
        }
    }

    @Override
    public void checkUsernameUnique(String userName) throws UserNameExistException {
        MemberDao memberDao = this.baseMapper;
        Integer username = memberDao.selectCount(new QueryWrapper<MemberEntity>().eq("username", userName));
        if (username > 0) {
            throw new UserNameExistException();
        }

    }

}
