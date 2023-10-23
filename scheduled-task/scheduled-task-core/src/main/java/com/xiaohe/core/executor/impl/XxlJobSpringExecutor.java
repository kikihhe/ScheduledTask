package com.xiaohe.core.executor.impl;

import com.xiaohe.core.annotation.XxlJobRegister;
import com.xiaohe.core.biz.AdminBiz;
import com.xiaohe.core.biz.client.AdminBizClient;
import com.xiaohe.core.executor.XxlJobExecutor;
import com.xiaohe.core.handler.annotation.XxlJob;
import com.xiaohe.core.model.Result;
import com.xiaohe.core.model.XxlJobInfo;
import com.xiaohe.core.util.CollectionUtil;
import com.xiaohe.core.util.XxlJobRemotingUtil;
import org.aopalliance.intercept.MethodInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-10-21 20:15
 */
@Configuration
public class XxlJobSpringExecutor extends XxlJobExecutor implements ApplicationContextAware, SmartInitializingSingleton, DisposableBean {
    private static Logger logger = LoggerFactory.getLogger(XxlJobSpringExecutor.class);

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        XxlJobSpringExecutor.applicationContext = applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {
        try {
            initJobHandlerMethodRepository(applicationContext);

            super.start();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    @Override
    public void destroy() throws Exception {
        super.stop();
    }

    private void initJobHandlerMethodRepository(ApplicationContext applicationContext) throws NoSuchMethodException {
        if (applicationContext == null) return;
        String[] beanDefinitionNames = applicationContext.getBeanNamesForType(Object.class, false, true);
        for (String beanDefinitionName : beanDefinitionNames) {
            Object bean = applicationContext.getBean(beanDefinitionName);
            Map<Method, XxlJob> annotatedMethods = null;
            Map<Method, XxlJobRegister> autoRegisterMethods = null;
            try {
                annotatedMethods = MethodIntrospector.selectMethods(bean.getClass(),
                        new MethodIntrospector.MetadataLookup<XxlJob>() {
                            @Override
                            public XxlJob inspect(Method method) {
                                return AnnotatedElementUtils.findMergedAnnotation(method, XxlJob.class);
                            }
                        }
                );
                for (Map.Entry<Method, XxlJob> methodXxlJobEntry : annotatedMethods.entrySet()) {
                    Method method = methodXxlJobEntry.getKey();
                    XxlJobRegister xxlJobRegister = (XxlJobRegister) method.getAnnotation(XxlJobRegister.class);
                    if (xxlJobRegister != null) {
                        autoRegisterMethods.put(method, xxlJobRegister);
                    }
                }
            } catch (Exception e) {
                logger.error("xxl-job method-jobhandler resolve error for bean[" + beanDefinitionName + "].", e);
            }
            if (CollectionUtil.isEmpty(annotatedMethods)) {
                continue;
            }
            // 将定时任务注册为JobHandler
            for (Map.Entry<Method, XxlJob> methodXxlJobEntry : annotatedMethods.entrySet()) {
                Method method = methodXxlJobEntry.getKey();
                XxlJob value = methodXxlJobEntry.getValue();
                registJobHandler(value, bean, method);
            }
            if (!CollectionUtil.isEmpty(autoRegisterMethods)) {
                autoRegister(autoRegisterMethods);
            }
        }

    }

    public void autoRegister(Map<Method, XxlJobRegister> autoRegisterMethods) {
        List<XxlJobInfo> autoRegister = new ArrayList<>();
        XxlJobInfo xxlJobInfo = new XxlJobInfo();
        for (Map.Entry<Method, XxlJobRegister> entry : autoRegisterMethods.entrySet()) {
            XxlJobRegister value = entry.getValue();
            xxlJobInfo.setJobGroup(value.jobGroup());
            xxlJobInfo.setJobDesc(value.jobDesc());
            xxlJobInfo.setAuthor(value.author());
            xxlJobInfo.setAlarmEmail(value.alarmEmail());
            xxlJobInfo.setScheduleType(value.scheduleType());
            xxlJobInfo.setScheduleConf(value.scheduleConf());
            xxlJobInfo.setMisfireStrategy(value.misfireStrategy());
            xxlJobInfo.setExecutorBlockStrategy(value.executorBlockStrategy());
            xxlJobInfo.setExecutorRouteStrategy(value.executorRouteStrategy());
            xxlJobInfo.setExecutorParam(value.executorParam());
            xxlJobInfo.setExecutorTimeout(value.executorTimeout());
            xxlJobInfo.setExecutorFailRetryCount(value.executorFailRetryCount());
            xxlJobInfo.setChildJobId(value.childJobId());
            xxlJobInfo.setTriggerLastTime(value.triggerStatus());
            autoRegister.add(xxlJobInfo);
        }
        for (AdminBizClient adminBizClient : getAdminBizClients()) {
            Result result = adminBizClient.registerMethod(autoRegister);
            if (result != null && result.getCode() == 200) {
                break;
            }
        }
        logger.info("method auto registry success: {}", autoRegister);
    }


    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
