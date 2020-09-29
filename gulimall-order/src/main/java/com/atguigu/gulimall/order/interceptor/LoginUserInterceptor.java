package com.atguigu.gulimall.order.interceptor;

import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.vo.MemberRsepVo;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 注意HandlerInterceptor和WebMvcConfigurer的配合使用
 * 拦截器和webmvc的配合使用，webmvc需要添加拦截器，在拦截器里配置拦截的条件
 */
@Component
public class LoginUserInterceptor implements HandlerInterceptor {

	public static ThreadLocal<MemberRsepVo> threadLocal = new ThreadLocal<>();


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //配置拦截的条件 只有登陆了才能通过返回true
        MemberRsepVo attribute = (MemberRsepVo) request.getSession().getAttribute(AuthServerConstant.LOGIN_USER);
        if (attribute != null) {
            threadLocal.set(attribute);
            return true;
        } else {
            //没登陆就去登录
            request.getSession().setAttribute("msg", AuthServerConstant.NOT_LOGIN);
            response.sendRedirect("http://auth.gulimall.com/login.html");
            return false;
        }
    }


//	@Override
//	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//
//		String uri = request.getRequestURI();
//		// 这个请求直接放行
//		boolean match = new AntPathMatcher().match("/order/order/status/**", uri);
//		if(match){
//			return true;
//		}
//		HttpSession session = request.getSession();
//		MemberRsepVo memberRsepVo = (MemberRsepVo) session.getAttribute(AuthServerConstant.LOGIN_USER);
//		if(memberRsepVo != null){
//			threadLocal.set(memberRsepVo);
//			return true;
//		}else{
//			// 没登陆就去登录
//			session.setAttribute("msg", AuthServerConstant.NOT_LOGIN);
//			response.sendRedirect("http://auth.glmall.com/login.html");
//			return false;
//		}
//	}
}
