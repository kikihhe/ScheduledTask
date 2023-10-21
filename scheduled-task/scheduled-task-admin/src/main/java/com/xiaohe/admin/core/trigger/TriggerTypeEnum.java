package com.xiaohe.admin.core.trigger;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-10-08 22:56
 */
public enum TriggerTypeEnum {
    MANUAL("jobconf_trigger_type_manual"),

    CRON("jobconf_trigger_type_cron"),

    RETRY("jobconf_trigger_type_retry"),

    PARENT("jobconf_trigger_type_parent"),

    API("jobconf_trigger_type_api"),

    /**
     * 这个任务没有被调度，别说指定时间前5s，哪怕又过了5s依旧没有被调度，就视为过期任务
     */
    MISFIRE("jobconf_trigger_type_misfire");

    private String title;

    TriggerTypeEnum(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
