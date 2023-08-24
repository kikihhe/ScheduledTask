package com.xiaohe.ScheduledTask.core.handler.impl;

import com.xiaohe.ScheduledTask.core.handler.IJobHandler;

import java.lang.reflect.Method;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-08-24 15:23
 */
public class MethodJobHandler extends IJobHandler {
    /**
     * 定时任务对应的bean对象
     */
    private Object target;
    /**
     * 定时任务方法
     */
    private Method method;

    /**
     * bean对象的初始化方法
     */
    private Method initMethod;

    /**
     * bean对象的销毁方法
     */
    private Method destroyMethod;

    /**
     * 执行定时任务
     * @throws Exception
     */
    @Override
    public void execute() throws Exception {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length > 0) {
            method.invoke(target, new Object[parameterTypes.length]);
        } else {
            method.invoke(target);
        }
    }

    /**
     * 反射调用目标对象的init方法
     * @throws Exception
     */
    @Override
    public void init() throws Exception {
        if(initMethod != null) {
            initMethod.invoke(target);
        }
    }

    /**
     * 反射调用目标对象的destroy方法
     * @throws Exception
     */
    @Override
    public void destroy() throws Exception {
        if(destroyMethod != null) {
            destroyMethod.invoke(target);
        }
    }

    public MethodJobHandler() {
    }

    public MethodJobHandler(Object target, Method method, Method initMethod, Method destroyMethod) {
        this.target = target;
        this.method = method;
        this.initMethod = initMethod;
        this.destroyMethod = destroyMethod;
    }

    public Object getTarget() {
        return target;
    }

    public void setTarget(Object target) {
        this.target = target;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Method getInitMethod() {
        return initMethod;
    }

    public void setInitMethod(Method initMethod) {
        this.initMethod = initMethod;
    }

    public Method getDestroyMethod() {
        return destroyMethod;
    }

    public void setDestroyMethod(Method destroyMethod) {
        this.destroyMethod = destroyMethod;
    }
}
