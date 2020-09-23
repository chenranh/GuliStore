package com.atguigu.gulimall.auth.vo;

import lombok.Data;

/**
 * 微博登录accessToken根据json转换的对象
 */
@Data
public class SocialUser {

    private String accessToken;

    private String remindIn;

    private int expiresIn;

    private String uid;

    private String isrealname;
}
