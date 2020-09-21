package com.atguigu.gulimall.auth.controller;

import cn.hutool.core.util.StrUtil;
import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.exception.BizCodeEnume;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.auth.feign.ThirdPartFeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.UUID;
import java.util.concurrent.TimeUnit;


/**
 * <p>Title: LoginController</p>
 * Description：登录注册模块
 */
@Slf4j
@Controller
public class LoginController {

    @Autowired
    ThirdPartFeignService thirdPartFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;


    @ResponseBody
    @GetMapping("/sms/sendcode")
    public R sendCode(@RequestParam("phone") String phone){

        // todo 1.接口防刷

        //防止同一个手机号在60秒内再次发送验证码
        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if(StrUtil.isNotBlank(redisCode)){
            long time = Long.parseLong(redisCode.split("_")[1]);
            if (System.currentTimeMillis()-time<60*1000) {
                //60秒类不能再发
                return R.error(BizCodeEnume.SMS_CODE_EXCEPTION.getCode(),BizCodeEnume.SMS_CODE_EXCEPTION.getMsg());
            }
        }

        //2.验证码的再次校验。存入redis 同时加入当前时间 存入类型 key-phone,value-code sms:code:13006862213--> 45696
        String code = UUID.randomUUID().toString().substring(0, 5)+"_"+System.currentTimeMillis();
        //redis缓存验证码
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX+phone,code,10, TimeUnit.MINUTES);
        //远程调用
        thirdPartFeignService.sendCode(phone);
        return R.ok();
    }


}
