package com.atguigu.gulimall.vo;

import lombok.Data;

/**
 * 微博登录accessToken根据json转换的对象
 */
@Data
public class SocialUser {

    private String accessToken;

    private String remindIn;

    private Long expiresIn;

    private String uid;

    private String isrealname;
}
