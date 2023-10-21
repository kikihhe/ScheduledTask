package com.xiaohe.core.executor.impl;

import com.xiaohe.core.executor.XxlJobExecutor;
import com.xiaohe.core.handler.annotation.XxlJob;
import com.xiaohe.core.util.CollectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-10-21 21:04
 */
public class XxlJobSimpleExecutor extends XxlJobExecutor {
    private static final Logger logger = LoggerFactory.getLogger(XxlJobSimpleExecutor.class);

    private List<Object> xxlJobBeanList = new ArrayList<>();

    @Override
    public void start() throws Exception {
        try {
            initJobHandlerMethodRepository(xxlJobBeanList);
            super.start();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    private void initJobHandlerMethodRepository(List<Object> xxlJobBeanList) throws NoSuchMethodException {
        if (CollectionUtil.isEmpty(xxlJobBeanList)) {
            return;
        }
        for (Object bean : xxlJobBeanList) {
            Method[] methods = bean.getClass().getDeclaredMethods();
            if (methods.length == 0) {
                continue;
            }
            for (Method method : methods) {
                XxlJob xxlJob = method.getAnnotation(XxlJob.class);
                registJobHandler(xxlJob, bean, method);
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
    }

    public List<Object> getXxlJobBeanList() {
        return xxlJobBeanList;
    }

    public void setXxlJobBeanList(List<Object> xxlJobBeanList) {
        this.xxlJobBeanList = xxlJobBeanList;
    }
}
