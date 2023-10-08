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

    MISFIRE("jobconf_trigger_type_misfire");

    private String title;

    TriggerTypeEnum(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
