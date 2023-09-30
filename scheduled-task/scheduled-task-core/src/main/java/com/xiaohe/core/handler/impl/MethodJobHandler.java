package com.xiaohe.core.handler.impl;

import com.xiaohe.core.handler.IJobHandler;

import java.lang.reflect.Method;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-09-30 20:57
 */
public class MethodJobHandler extends IJobHandler {
    /**
     * 目标对象的class对象
     */
    private final Object target;

    /**
     * 需要执行的方法
     */
    private final Method method;

    /**
     * 该对象中的初始化方法
     */
    private Method initMethod;

    /**
     * 该对象中的销毁方法
     */
    private Method destroyMethod;


    public MethodJobHandler(Object target, Method method, Method initMethod, Method destroyMethod) {
        this.target = target;
        this.method = method;
        this.initMethod = initMethod;
        this.destroyMethod = destroyMethod;
    }

    @Override
    public void execute() throws Exception {
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
        return super.toString()+"["+ target.getClass() + "#" + method.getName() +"]";
    }
}
