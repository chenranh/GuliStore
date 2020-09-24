package com.atguigu.gulimallcart.vo;

import lombok.Data;
import lombok.ToString;

/**
 * 传输对象
 */
@Data
@ToString
public class UserInfoTo {
    private Long userId;
    private String userKey;
    //判断是不是临时用户 cookie中有就是true
    private boolean tempUser=false;
}
