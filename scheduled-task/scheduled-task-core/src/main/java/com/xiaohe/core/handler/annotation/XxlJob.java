package com.xiaohe.core.handler.annotation;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-10-01 15:37
 */
public @interface XxlJob {
    String value();

    String initMethod() default "";

    String destroyMethod() default "";
}
