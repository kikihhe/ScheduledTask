package com.xiaohe.admin.core.alarm;

import com.xiaohe.admin.core.model.XxlJobInfo;
import com.xiaohe.admin.core.model.XxlJobLog;
import com.xiaohe.core.util.CollectionUtil;
import org.codehaus.groovy.transform.ASTTestTransformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-10-05 21:00
 */
public class JobAlarmer implements ApplicationContextAware, InitializingBean {
    private static Logger logger = LoggerFactory.getLogger(JobAlarmer.class);
    private ApplicationContext applicationContext;
    private List<JobAlarm> jobAlarmList;
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


    /**
     * 容器装填好后将所有告警器装入JobAlarmList
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, JobAlarm> serviceBean =
                applicationContext.getBeansOfType(JobAlarm.class);
        if (!CollectionUtil.isEmpty(serviceBean)) {
            jobAlarmList = new ArrayList<JobAlarm>(serviceBean.values());
        }
    }


    /**
     * 供外界调用的告警 (其实只有JobFailMonitorHelper调用)
     * @param info
     * @param xxlJobLog
     * @return
     */
    public boolean alarm(XxlJobInfo info, XxlJobLog xxlJobLog) {
        boolean result = true;
        if (CollectionUtil.isEmpty(jobAlarmList)) {
            return false;
        }
        // 只要有一个发送失败，此次告警就算失败，但是其他地方的告警还是要继续发。
        for (JobAlarm alarm : jobAlarmList) {
            boolean resultItem = false;
            try {
                resultItem = alarm.doAlarm(info, xxlJobLog);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            if (!resultItem) {
                result = false;
            }
        }
        return result;
    }

}
