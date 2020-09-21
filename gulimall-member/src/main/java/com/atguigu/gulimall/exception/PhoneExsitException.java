package com.atguigu.gulimall.exception;

/**
 * @title: PhoneExsitException
 * @Author yuke
 * @Date: 2020-09-21 18:26
 */
public class PhoneExsitException extends RuntimeException {
    public PhoneExsitException() {
        super("用户名存在");
    }
}
