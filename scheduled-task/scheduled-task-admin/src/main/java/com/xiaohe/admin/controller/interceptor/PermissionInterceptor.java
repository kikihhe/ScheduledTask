package com.xiaohe.admin.controller.interceptor;

import com.xiaohe.admin.controller.annotation.PermissionLimit;
import com.xiaohe.admin.core.util.I18nUtil;
import com.xiaohe.admin.service.LoginService;
import com.xiaohe.admin.core.model.XxlJobUser;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * @author : 小何
 * @Description : 权限过滤器
 * @date : 2023-09-21 22:45
 */
@Component
public class PermissionInterceptor implements AsyncHandlerInterceptor {
    @Resource
    private LoginService loginService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        boolean needLogin = true;
        boolean needAdminuser = false;
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        PermissionLimit permission = handlerMethod.getMethodAnnotation(PermissionLimit.class);
        if (permission != null) {
            needLogin = permission.limit();
            needAdminuser = permission.adminuser();
        }
        // 如果不需要登录，直接pass
        if (!needLogin) {
            return true;
        }
        XxlJobUser loginUser = loginService.ifLogin(request, response);
        // 如果没有登陆，重定向到登录界面
        if (loginUser == null) {
            response.setStatus(302);
            response.setHeader("location", request.getContextPath() + "/toLogin");
            return false;
        }
        // 如果需要管理员但不是，抛出异常
        if (needAdminuser && loginUser.getRole() != 1) {
            throw new RuntimeException(I18nUtil.getString("system_permission_limit"));
        }
        // 判断是否登录时将用户从request中取了出来。重新塞进去
        request.setAttribute(LoginService.LOGIN_IDENTITY_KEY, loginUser);
        return true;
    }
}
