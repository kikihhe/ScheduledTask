package com.xiaohe.admin.core.trigger;

import com.xiaohe.admin.core.util.I18nUtil;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-10-08 22:56
 */
public enum TriggerTypeEnum {
    MANUAL(I18nUtil.getString("jobconf_trigger_type_manual")),
    CRON(I18nUtil.getString("jobconf_trigger_type_cron")),
    RETRY(I18nUtil.getString("jobconf_trigger_type_retry")),
    PARENT(I18nUtil.getString("jobconf_trigger_type_parent")),
    API(I18nUtil.getString("jobconf_trigger_type_api")),
    /**
     * 这个任务没有被调度，别说指定时间前5s，哪怕又过了5s依旧没有被调度，就视为过期任务
     */
    MISFIRE(I18nUtil.getString("jobconf_trigger_type_misfire"));

    private String title;

    TriggerTypeEnum(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
