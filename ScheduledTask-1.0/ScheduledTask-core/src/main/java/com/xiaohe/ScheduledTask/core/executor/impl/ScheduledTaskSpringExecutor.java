package com.xiaohe.ScheduledTask.core.executor.impl;

import com.xiaohe.ScheduledTask.core.executor.ScheduledTaskExecutor;
import com.xiaohe.ScheduledTask.core.handler.annotation.ScheduledTask;
import com.xiaohe.ScheduledTask.core.util.CollectionUtil;
import com.xiaohe.ScheduledTask.core.util.ObjectUtil;
import com.xiaohe.ScheduledTask.core.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author : 小何
 * @Description : 执行器扫描所有加了 @ScheduledTask注解 的方法，将其加入Map中，等待调度。
 * ScheduledTaskSpringExecutor 是基于Spring实现的。
 * @date : 2023-08-24 14:41
 */
public class ScheduledTaskSpringExecutor extends ScheduledTaskExecutor implements ApplicationContextAware, SmartInitializingSingleton, DisposableBean {
    private static Logger logger = LoggerFactory.getLogger(ScheduledTaskSpringExecutor.class);

    private static ApplicationContext applicationContext;

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * ApplicationContextAware需要实现的方法，这个方法把Spring容器给我们，可以把它取出来拿去用
     *
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 将Spring的容器取出来。
        ScheduledTaskSpringExecutor.applicationContext = applicationContext;
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    /**
     * Spring的所有容器加载完后调用此方法，在此方法内，将所有加了@ScheduledTask注解的类+方法取出来
     */
    @Override
    public void afterSingletonsInstantiated() {
        // 已经获取spring容器，取出所有加了注解的类+方法
        initJobHandlerMethodRepository(applicationContext);
        // 调用父类的方法启动执行器
        super.start();

    }

    /**
     * 将所有加了@ScheduledTask注解的类+方法取出来
     *
     * @param applicationContext
     */
    private void initJobHandlerMethodRepository(ApplicationContext applicationContext) {
        if (ObjectUtil.isNull(applicationContext)) {
            return;
        }
        // 获取容器中所有bean的名字
        // includeNonSingletons : 是否获取非单例对象
        // allowEagerInit : 是否获取延迟加载的对象
        String[] beanDefinitionNames = applicationContext.getBeanNamesForType(Object.class, false, true);
        for (String beanName : beanDefinitionNames) {
            // 根据名字获取bean
            Object bean = applicationContext.getBean(beanName);
            Map<Method, ScheduledTask> annotatedMethods = null;
            // 查找该类的所有方法是否含有 ScheduledTask 注解的方法
            annotatedMethods = MethodIntrospector.selectMethods(
                    bean.getClass(),
                    new MethodIntrospector.MetadataLookup<ScheduledTask>() {
                        @Override
                        public ScheduledTask inspect(Method method) {
                            return AnnotatedElementUtils.findMergedAnnotation(method, ScheduledTask.class);
                        }
                    }
            );
            if (CollectionUtil.isEmpty(annotatedMethods)) {
                continue;
            }
            // 遍历注册该bean对象中所有加了注解的方法
            for (Map.Entry<Method, ScheduledTask> methodEntry : annotatedMethods.entrySet()) {
                Method method = methodEntry.getKey();
                ScheduledTask annotation = methodEntry.getValue();
                regisJobHandler(annotation, bean, method);
            }


        }


    }



}
