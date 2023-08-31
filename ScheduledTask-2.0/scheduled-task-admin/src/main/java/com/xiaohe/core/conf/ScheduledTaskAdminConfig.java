package com.xiaohe.core.conf;

import com.xiaohe.mapper.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;

/**
 * @author : 小何
 * @Description : 公共配置类
 * @date : 2023-08-31 16:57
 */
@Component
public class ScheduledTaskAdminConfig {

    private static final ScheduledTaskAdminConfig adminConfig = null;

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

    public ScheduledTaskUserMapper getScheduledTaskUserMapper() {
        return scheduledTaskUserMapper;
    }
}
