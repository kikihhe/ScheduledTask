package com.xiaohe.admin.controller.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author : 小何
 * @Description : 权限注解，放在方法上，运行时检验
 * @date : 2023-09-21 22:36
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PermissionLimit {
    /**
     * 是否拦截，默认为true
     * @return
     */
    boolean limit() default true;

    /**
     * 是否为管理员才能执行的方法，默认为false
     * @return
     */
    boolean adminuser() default false;

}
