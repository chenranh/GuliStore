package com.atguigu.gulimall.auth.controller;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.exception.BizCodeEnume;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRsepVo;
import com.atguigu.gulimall.auth.feign.MemberFeignService;
import com.atguigu.gulimall.auth.feign.ThirdPartFeignService;
import com.atguigu.gulimall.auth.vo.UserLoginVo;
import com.atguigu.gulimall.auth.vo.UserRegistVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 *账号密码登录
 */
@Slf4j
@Controller
public class LoginController {

    @Autowired
    ThirdPartFeignService thirdPartFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    MemberFeignService memberFeignService;

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

        String code = UUID.randomUUID().toString().substring(0, 5);
        //存入redis 同时加入当前时间 存入类型 key-phone,value-code sms:code:13006862213--> 45696
        String substring =code+"_"+System.currentTimeMillis();
        //redis缓存验证码
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX+phone,substring,10, TimeUnit.MINUTES);
        //远程调用
        thirdPartFeignService.sendCode(phone,code);
        return R.ok();
    }


    /**
     *  //todo 重定向携带数据，利用session原理。将数据放在session中，只要跳到下一个页面取出这个数据后，session中的数据就会删除掉
     *  //todo 分布式下的session问题
     * @param vo
     * @param result
     * @param redirectAttributes 模拟重定向携带数据
     * @return
     */
    @PostMapping("/regist")
    public String regist(@Valid UserRegistVo vo, BindingResult result, RedirectAttributes redirectAttributes, HttpSession session){

        //1.校验前端传递过来的参数
        if (result.hasErrors()){
            Map<String, String> errors = result.getFieldErrors().stream()
                    .collect(Collectors.toMap(FieldError::getField, DefaultMessageSourceResolvable::getDefaultMessage));

            redirectAttributes.addFlashAttribute("errors",errors);
            //校验出错，转发到注册页
            return "redirect:http://auth.gulimall.com/reg.html";
        }

        // 2.校验验证码
        String code = vo.getCode();
        String s = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
        if (StrUtil.isNotBlank(s)){
            String s1 = s.split("_")[0];
            if (code.equals(s1)){
                //删除验证码(令牌机制  用过以后删掉)
                redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
                //验证码通过 真正注册 调用远程服务进行注册
                R r = memberFeignService.regist(vo);
                if (r.getCode()==0){
                    //成功
                    return  "redirect:http://auth.gulimall.com/login.html";
                }else {
                    HashMap<String, String> errors = new HashMap<>();
                    errors.put("msg",r.getData(new TypeReference<String>(){}));
                    redirectAttributes.addFlashAttribute("errors", errors);
                    return  "redirect:http://auth.gulimall.com/reg.html";
                }

            }else {
                HashMap<String, String> errors = new HashMap<>();
                errors.put("code", "验证码错误");
                return "redirect:http://auth.gulimall.com/reg.html";
            }
        }else {
            HashMap<String, String> errors = new HashMap<>();
            errors.put("code", "验证码错误");
            return "redirect:http://auth.gulimall.com/reg.html";
        }

    }

    @PostMapping("/login")
    public String login(UserLoginVo userLoginVo, RedirectAttributes redirectAttributes, HttpSession session){
        // 远程登录
        R r = memberFeignService.login(userLoginVo);
        if(r.getCode() == 0){
            // 登录成功
            MemberRsepVo rsepVo = r.getData("data", new TypeReference<MemberRsepVo>() {});
            session.setAttribute(AuthServerConstant.LOGIN_USER, rsepVo);
            log.info("\n欢迎 [" + rsepVo.getUsername() + "] 登录");
            return "redirect:http://gulimall.com";
        }else {
            HashMap<String, String> errors = new HashMap<>();
            // 获取错误信息
            errors.put("msg", r.getData("msg",new TypeReference<String>(){}));
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }


    @GetMapping("/login.html")
    public String loginPage(HttpSession session){
        Object attribute = session.getAttribute(AuthServerConstant.LOGIN_USER);
        if (attribute==null){
            //没登陆
            return "login";
        }else {
            return "redirect:http://gulimall.com";
        }

    }

}
