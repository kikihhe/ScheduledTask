package com.xiaohe.core.conf;

import com.xiaohe.core.scheduler.TaskScheduler;
import com.xiaohe.mapper.*;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.Arrays;

/**
 * @author : 小何
 * @Description : 公共配置类
 * @date : 2023-08-31 16:57
 */
@Component
public class ScheduledTaskAdminConfig implements InitializingBean, DisposableBean {

    private static ScheduledTaskAdminConfig adminConfig = null;

    public static ScheduledTaskAdminConfig getAdminConfig() {
        return adminConfig;

    }






    @Value("${scheduled.task.i18n}")
    private String i18n;

    @Value("${scheduled.task.accessToken}")
    private String accessToken;

    @Value("${spring.mail.from}")
    private String emailFrom;

    @Value("${scheduled.task.triggerpool.fast.max}")
    private int triggerPoolFastMax;

    @Value("${scheduled.task.triggerpool.slow.max}")
    private int triggerPoolSlowMax;

    @Value("${scheduled.task.logretentiondays}")
    private int logretentiondays;

    @Resource
    private ScheduledTaskGroupMapper scheduledTaskGroupMapper;

    @Resource
    private ScheduledTaskInfoMapper scheduledTaskInfoMapper;

    @Resource
    private ScheduledTaskLogMapper scheduledTaskLogMapper;

    @Resource
    private ScheduledTaskRegistryMapper scheduledTaskRegistryMapper;

    @Resource
    private ScheduledTaskUserMapper scheduledTaskUserMapper;

    @Resource
    private DataSource dataSource;

    private TaskScheduler scheduler;

    @Resource
    private JavaMailSender mailSender;




    /**
     * 所有容器初始化后执行
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        adminConfig = this;
        // 初始化各种组件
        scheduler = new TaskScheduler();
        scheduler.init();
    }

    /**
     * 容器销毁时执行
     * @throws Exception
     */
    @Override
    public void destroy() throws Exception {
        // 结束各种组件，回收资源
        scheduler.destroy();
    }



    public String getI18n() {
        if (!Arrays.asList("zh_CN", "zh_TC", "en").contains(i18n)) {
            return "zh_CN";
        }
        return i18n;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getEmailFrom() {
        return emailFrom;
    }

    public int getTriggerPoolFastMax() {
        return triggerPoolFastMax < 200 ? 200 : triggerPoolFastMax;
    }

    public int getTriggerPoolSlowMax() {
        return triggerPoolSlowMax < 100 ? 100 : triggerPoolSlowMax;
    }

    public int getLogretentiondays() {
        return logretentiondays;
    }

    public ScheduledTaskGroupMapper getScheduledTaskGroupMapper() {
        return scheduledTaskGroupMapper;
    }

    public ScheduledTaskInfoMapper getScheduledTaskInfoMapper() {
        return scheduledTaskInfoMapper;
    }

    public ScheduledTaskLogMapper getScheduledTaskLogMapper() {
        return scheduledTaskLogMapper;
    }

    public ScheduledTaskRegistryMapper getScheduledTaskRegistryMapper() {
        return scheduledTaskRegistryMapper;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public ScheduledTaskUserMapper getScheduledTaskUserMapper() {

        return scheduledTaskUserMapper;
    }

    public JavaMailSender getMailSender() {
        return mailSender;
    }

    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
}
