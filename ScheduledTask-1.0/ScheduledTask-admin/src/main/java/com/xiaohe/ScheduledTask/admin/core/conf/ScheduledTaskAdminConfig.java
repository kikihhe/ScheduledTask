package com.xiaohe.ScheduledTask.admin.core.conf;

import com.xiaohe.ScheduledTask.admin.core.scheduler.ScheduledTaskScheduler;
import com.xiaohe.ScheduledTask.admin.dao.ScheduledTaskGroupMapper;
import com.xiaohe.ScheduledTask.admin.dao.ScheduledTaskInfoMapper;
import com.xiaohe.ScheduledTask.admin.dao.ScheduledTaskRegistryMapper;
import com.xiaohe.ScheduledTask.admin.dao.ScheduledTaskUserMapper;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author : 小何
 * @Description : 配置类
 * @date : 2023-08-25 12:08
 */
@Component
public class ScheduledTaskAdminConfig implements InitializingBean, DisposableBean {
    private static ScheduledTaskAdminConfig adminConfig = null;

    public static ScheduledTaskAdminConfig getAdminConfig() {
        return adminConfig;
    }

    private ScheduledTaskScheduler taskScheduler;

    /**
     * 在所有bean初始化后会回调这个方法，在里面进行 ScheduledTask的所有组件的胡初始化
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        adminConfig = this;
        taskScheduler = new ScheduledTaskScheduler();
        taskScheduler.init();
    }

    /**
     * 当前类的对象被Spring销毁时会调用这个类
     * @throws Exception
     */
    @Override
    public void destroy() throws Exception {
        taskScheduler.destory();
    }

    @Value("${scheduledtask.i18n}")
    private String i18n;

    @Value("${scheduledtask.accessToken}")
    private String accessToken;

    @Value("${spring.mail.from}")
    private String emailFrom;

    /**
     * 快线程池的最大线程数
     */
    @Value("${scheduledtask.triggerpool.fast.max}")
    private int triggerPoolFastMax;

    /**
     * 慢线程池的最大线程数
     */
    @Value("${scheduledtask.triggerpool.slow.max}")
    private int triggerPoolSlowMax;

    /**
     * 该属性是日志保留时间的意思
     */
    @Value("${scheduledtask.logretentiondays}")
    private int logretentiondays;

    // 注入各种mapper类
    @Resource
    private ScheduledTaskGroupMapper scheduledTaskGroupMapper;

    @Resource
    private ScheduledTaskInfoMapper scheduledTaskInfoMapper;

    @Resource
    private ScheduledTaskUserMapper scheduledTaskUserMapper;

    @Resource
    private ScheduledTaskRegistryMapper scheduledTaskRegistryMapper;

    public static void setAdminConfig(ScheduledTaskAdminConfig adminConfig) {
        ScheduledTaskAdminConfig.adminConfig = adminConfig;
    }

    public ScheduledTaskScheduler getTaskScheduler() {
        return taskScheduler;
    }

    public void setTaskScheduler(ScheduledTaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    public String getI18n() {
        return i18n;
    }

    public void setI18n(String i18n) {
        this.i18n = i18n;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getEmailFrom() {
        return emailFrom;
    }

    public void setEmailFrom(String emailFrom) {
        this.emailFrom = emailFrom;
    }

    public int getTriggerPoolFastMax() {
        if (triggerPoolFastMax < 200) {
            return 200;
        }
        return triggerPoolFastMax;
    }

    public void setTriggerPoolFastMax(int triggerPoolFastMax) {
        this.triggerPoolFastMax = triggerPoolFastMax;
    }

    public int getTriggerPoolSlowMax() {
        if (triggerPoolSlowMax < 100) {
            return 100;
        }
        return triggerPoolSlowMax;
    }

    public void setTriggerPoolSlowMax(int triggerPoolSlowMax) {
        this.triggerPoolSlowMax = triggerPoolSlowMax;
    }

    public int getLogretentiondays() {
        if (logretentiondays < 7) {
            return -1;
        }
        return logretentiondays;
    }

    public void setLogretentiondays(int logretentiondays) {
        this.logretentiondays = logretentiondays;
    }

    public ScheduledTaskGroupMapper getScheduledTaskGroupMapper() {
        return scheduledTaskGroupMapper;
    }

    public void setScheduledTaskGroupMapper(ScheduledTaskGroupMapper scheduledTaskGroupMapper) {
        this.scheduledTaskGroupMapper = scheduledTaskGroupMapper;
    }

    public ScheduledTaskInfoMapper getScheduledTaskInfoMapper() {
        return scheduledTaskInfoMapper;
    }

    public void setScheduledTaskInfoMapper(ScheduledTaskInfoMapper scheduledTaskInfoMapper) {
        this.scheduledTaskInfoMapper = scheduledTaskInfoMapper;
    }

    public ScheduledTaskUserMapper getScheduledTaskUserMapper() {
        return scheduledTaskUserMapper;
    }

    public void setScheduledTaskUserMapper(ScheduledTaskUserMapper scheduledTaskUserMapper) {
        this.scheduledTaskUserMapper = scheduledTaskUserMapper;
    }

    public ScheduledTaskRegistryMapper getScheduledTaskRegistryMapper() {
        return scheduledTaskRegistryMapper;
    }

    public void setScheduledTaskRegistryMapper(ScheduledTaskRegistryMapper scheduledTaskRegistryMapper) {
        this.scheduledTaskRegistryMapper = scheduledTaskRegistryMapper;
    }
}
