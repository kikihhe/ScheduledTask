package com.xiaohe.admin.controller.interceptor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * @author : 小何
 * @Description : 配置拦截器
 * @date : 2023-09-22 10:21
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Resource
    private CookieInterceptor cookieInterceptor;
    @Resource
    private PermissionInterceptor permissionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(cookieInterceptor);
        registry.addInterceptor(permissionInterceptor);
    }
}
