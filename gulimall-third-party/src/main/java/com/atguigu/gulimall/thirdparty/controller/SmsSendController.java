package com.atguigu.gulimall.thirdparty.controller;

import com.atguigu.common.exception.BizCodeEnume;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.thirdparty.component.SmsComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sms")
public class SmsSendController {

    @Autowired
    SmsComponent smsComponent;

    /**
     * 提供给别的服务进行调用的
     */
    @GetMapping("/sendcode")
    public R sendCode(@RequestParam("phone") String phone, @RequestParam("code") String code) {
        if (!"fail".equals(smsComponent.sendSmsCode(phone, code).split("_")[0])) {
            return R.ok();
        }
        return R.error(BizCodeEnume.SMS_SEND_CODE_EXCEPTION.getCode(), BizCodeEnume.SMS_SEND_CODE_EXCEPTION.getMsg());
    }


}
