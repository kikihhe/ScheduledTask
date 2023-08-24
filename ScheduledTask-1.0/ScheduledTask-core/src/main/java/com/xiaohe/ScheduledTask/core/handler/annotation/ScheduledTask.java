package com.xiaohe.ScheduledTask.core.handler.annotation;

import java.lang.annotation.*;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-08-24 15:02
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

    /**
     * 初始化方法
     * @return
     */
    String init() default "";

    /**
     * 销毁方法
     * @return
     */
    String destroy() default "";

}


