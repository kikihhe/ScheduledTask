package com.xiaohe.handler.impl;

import com.xiaohe.handler.IJobHandler;

import java.lang.reflect.Method;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-09-08 11:38
 */
public class MethodJobHandler extends IJobHandler {
    /**
     * 目标类对象
     */
    private final Object target;

    /**
     * 目标执行方法
     */
    private final Method method;

    /**
     * 初始化方法
     */
    private Method initMethod;

    /**
     * 销毁方法
     */
    private Method destroyMethod;

    public MethodJobHandler(Object target, Method method, Method initMethod, Method destroyMethod) {
        this.target = target;
        this.method = method;
        this.initMethod = initMethod;
        this.destroyMethod = destroyMethod;
    }

    @Override
    public void executor() throws Exception {
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length > 0) {
            method.invoke(target, new Object[paramTypes.length]);
        } else {
            method.invoke(target);
        }

    }

    @Override
    public void init() throws Exception {
        if (initMethod != null) {
            initMethod.invoke(target);
        }
    }

    @Override
    public void destroy() throws Exception {
        if (destroyMethod != null) {
            destroyMethod.invoke(target);
        }
    }

    @Override
    public String toString() {
        return super.toString() + "[" + target.getClass() + "#" + method.getName() + "]";
    }
}
