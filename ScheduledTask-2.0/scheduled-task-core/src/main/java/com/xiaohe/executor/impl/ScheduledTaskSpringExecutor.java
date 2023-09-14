package com.xiaohe.executor.impl;

import com.xiaohe.executor.ScheduledTaskExecutor;
import com.xiaohe.handler.annotation.ScheduledTask;
import org.apache.groovy.util.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author : 小何
 * @Description : 对接Spring框架的执行器
 * @date : 2023-09-14 12:39
 */
public class ScheduledTaskSpringExecutor extends ScheduledTaskExecutor implements ApplicationContextAware, SmartInitializingSingleton, DisposableBean {
    private static Logger logger = LoggerFactory.getLogger(ScheduledTaskSpringExecutor.class);


    private ApplicationContext applicationContext;


    /**
     * 初始化所有定时任务的bean, 调用父类registJobHandler
     * @param applicationContext
     */
    private void initJobHandlerMethodRepository(ApplicationContext applicationContext) throws NoSuchMethodException {
        if (applicationContext == null) {
            return;
        }
        // 获取容器中所有bean
        // 第一个false: 不获取非单例对象 (只获取单例对象)
        // 第二个true: 懒加载的bean也获取
        String[] beanDefinitionNames = applicationContext.getBeanNamesForType(Object.class, false, true);

        for (String beanDefinitionName : beanDefinitionNames) {
            Object bean = applicationContext.getBean(beanDefinitionName);
            // 将这个bean中所有加了 @ScheduledTask 注解的方法提取出来 
            Map<Method, ScheduledTask> annotatedMethods = null;
            try {
                annotatedMethods = MethodIntrospector.selectMethods(bean.getClass(), new MethodIntrospector.MetadataLookup<ScheduledTask>() {
                    @Override
                    public ScheduledTask inspect(Method method) {
                        return AnnotatedElementUtils.findMergedAnnotation(method, ScheduledTask.class);
                    }
                });
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            // 如果没有，下一个
            if (CollectionUtils.isEmpty(annotatedMethods)) {
                continue;
            }
            // 将这个bean中含有定时任务的方法全部注册进 jobHandlerRepository
            for (Map.Entry<Method, ScheduledTask> entry : annotatedMethods.entrySet()) {
                Method method = entry.getKey();
                ScheduledTask annotation = entry.getValue();
                registJobHandler(method, bean, annotation);

            }

        }


    }


    @Override
    public void destroy() throws Exception {
        super.destroy();

    }

    /**
     * 所有bean初始化后调用这个方法
     */
    @Override
    public void afterSingletonsInstantiated() {
        try {
            initJobHandlerMethodRepository(applicationContext);
        } catch (NoSuchMethodException e) {
            logger.error(e.getMessage(), e);
        }

        try {
            super.start();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;

    }
}
