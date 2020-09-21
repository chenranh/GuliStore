package com.atguigu.gulimall.exception;

/**
 * @title: UserNameExistException
 * @Author yuke
 * @Date: 2020-09-21 18:27
 */
public class UserNameExistException extends RuntimeException{
    public UserNameExistException() {
        super("手机号存在");
    }
}
