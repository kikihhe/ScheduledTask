package com.xiaohe.executor.impl;

import com.xiaohe.executor.ScheduledTaskExecutor;
import com.xiaohe.handler.annotation.ScheduledTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author : 小何
 * @Description : 不依赖Spring的执行器
 * @date : 2023-09-20 12:13
 */
public class ScheduledTaskSimpleExecutor extends ScheduledTaskExecutor {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskSimpleExecutor.class);

    /**
     * 存放所有定时任务对象的集合
     */
    private List<Object> beanList = new ArrayList<>();

    @Override
    public void start() {
        try {
            initJobHandlerMethodRepository(beanList);
        } catch (NoSuchMethodException e) {
            logger.error(e.getMessage(), e);
        }
        super.start();
    }

    private void initJobHandlerMethodRepository(List<Object> beanList) throws NoSuchMethodException {
        if (CollectionUtils.isEmpty(beanList)) {
            return;
        }
        for (Object bean : beanList) {
            Method[] methods = bean.getClass().getDeclaredMethods();
            if (methods.length == 0) {
                continue;
            }
            for (Method method : methods) {
                ScheduledTask annotation = method.getAnnotation(ScheduledTask.class);
                if (annotation != null) {
                    registJobHandler(method, bean, annotation);
                }
            }
        }

    }

    public List<Object> getBeanList() {
        return beanList;
    }

    public void setBeanList(List<Object> beanList) {
        this.beanList = beanList;
    }
}
