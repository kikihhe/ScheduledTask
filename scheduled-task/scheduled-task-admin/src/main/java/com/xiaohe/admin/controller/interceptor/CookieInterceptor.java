package com.xiaohe.admin.controller.interceptor;

import com.xiaohe.admin.core.util.FtlUtil;
import com.xiaohe.admin.core.util.I18nUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

/**
 * @author : 小何
 * @Description : 将所有Cookie从请求中放入modelAndView中
 * @date : 2023-09-22 10:17
 */
@Component
public class CookieInterceptor implements AsyncHandlerInterceptor {
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        if (modelAndView != null && request.getCookies() != null && request.getCookies().length > 0) {
            HashMap<String, Cookie> cookieMap = new HashMap<>();
            for (Cookie cookie : request.getCookies()) {
                cookieMap.put(cookie.getName(), cookie);
            }
            modelAndView.addObject("cookieMap", cookieMap);
        }
        if (modelAndView != null) {
            modelAndView.addObject("I18nUtil", FtlUtil.generateStaticModel(I18nUtil.class.getName()));
        }
    }
}
