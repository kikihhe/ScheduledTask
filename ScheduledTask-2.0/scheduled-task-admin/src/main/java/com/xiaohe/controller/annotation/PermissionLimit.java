package com.xiaohe.controller.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author : 小何
 * @Description : 权限注解，只能用于方法上, 运行时生效
 * @date : 2023-09-05 13:09
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PermissionLimit {

    /**
     * 是否拦截，默认拦截
     * @return
     */
    boolean limit() default true;

    /**
     * 是否为管理员登录，默认为false
     * @return
     */
    boolean adminuser() default false;
}
