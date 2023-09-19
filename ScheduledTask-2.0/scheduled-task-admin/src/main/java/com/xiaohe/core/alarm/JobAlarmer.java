package com.xiaohe.core.alarm;

import com.xiaohe.core.model.ScheduledTaskInfo;
import com.xiaohe.core.model.ScheduledTaskLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author : 小何
 * @Description : 发送邮件的
 * @date : 2023-09-17 16:15
 */
@Component
public class JobAlarmer implements ApplicationContextAware, InitializingBean {
    private static Logger logger = LoggerFactory.getLogger(JobAlarmer.class);
    /**
     * spring容器
     */
    private ApplicationContext applicationContext;
    /**
     * 邮件报警器的集合
     */
    private List<JobAlarm> jobAlarmList;

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, JobAlarm> serviceBeanMap = applicationContext.getBeansOfType(JobAlarm.class);
        if (!CollectionUtils.isEmpty(serviceBeanMap)) {
            jobAlarmList = new ArrayList<>(serviceBeanMap.values());
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 在 JobFailMonitorHelper 中被调用的方法
     *
     * @param jobInfo
     * @param jobLog
     */
    public boolean alarm(ScheduledTaskInfo jobInfo, ScheduledTaskLog jobLog) {
        if (CollectionUtils.isEmpty(jobAlarmList)) {
            return false;
        }
        for (JobAlarm alarm : jobAlarmList) {
            // 要求是每一个邮箱都要收到邮件，有一个没有收到就设置为false
            boolean result = alarm.doAlarm(jobInfo, jobLog);
            if (!result) {
                return false;
            }
        }
        return true;

    }
}
