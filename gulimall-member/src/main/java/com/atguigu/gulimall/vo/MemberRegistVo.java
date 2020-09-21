package com.atguigu.gulimall.vo;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

/**
 * @title: UserRegistVo
 * @Author yuke
 * @Date: 2020-09-21 9:55
 * 前端传过来的对象  需要做验证
 */
@Data
public class MemberRegistVo {

    private String userName;

    private String password;

    private String phone;

}
