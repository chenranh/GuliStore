package com.atguigu.gulimall.ssoserver.controller;

import com.atguigu.gulimall.ssoserver.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <p>Title: LoginController</p>
 * Description：
 */
@Controller
public class LoginController {

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	/**
	 * 根据token查询用户信息
	 * @param token
	 * @return
	 */
	@ResponseBody
	@GetMapping("/userInfo")
	public String userInfo(@RequestParam("token") String token){
		String s = stringRedisTemplate.opsForValue().get(token);
		return s;
	}

	@GetMapping("/login.html")
	public String loginPage(User user, Model model, @CookieValue(value = "sso_token",required = false) String sso_token){
		if(!StringUtils.isEmpty(sso_token)){
			// 有人登录过 浏览器留下的有sso_token 直接返回之前页面
			return "redirect:" + user.getUrl() + "?username=" + user.getUsername() + "&token=" + sso_token;
		}
		model.addAttribute("url", user.getUrl());
		model.addAttribute("username", user.getUsername());
		return "login";
	}

	@PostMapping("/doLogin")//点击登录 从页面跳转过来
	public String doLogin(User user, HttpServletResponse response){
		if(!StringUtils.isEmpty(user.getUsername()) && !StringUtils.isEmpty(user.getPassword()) && "fire".equals(user.getUsername()) && "fire".equals(user.getPassword())){
			// 登录成功跳转 跳回之前的页面
			String uuid = UUID.randomUUID().toString().replace("-", "");
			Cookie cookie = new Cookie("sso_token", uuid);
			response.addCookie(cookie);
			//把登录成功的用户保存到redis  由客户端取出
			stringRedisTemplate.opsForValue().set( uuid, user.getUser(),30, TimeUnit.MINUTES);
			//浏览器访问自己页面
			return "redirect:" + user.getUrl() + "?username=" + user.getUser() + "&token=" + uuid;
		}
		// 登录失败
		return "login";
	}
}
