package com.xiaohe.handler.annotation;

import java.lang.annotation.*;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-09-14 12:45
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ScheduledTask {
    /**
     * 定时任务的名称
     * @return
     */
    String value();

    String destroy() default "";

    String init() default "";
}
