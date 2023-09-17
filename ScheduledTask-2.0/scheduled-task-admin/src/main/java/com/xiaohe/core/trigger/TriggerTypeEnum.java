package com.xiaohe.core.trigger;

/**
 * @author : 小何
 * @Description : 触发类型，一般为corn
 * @date : 2023-09-01 12:45
 */
public enum TriggerTypeEnum {
    MANUAL("jobconf_trigger_type_manual"),
    CRON("jobconf_trigger_type_cron"),
    MISFIRE("jobconf_trigger_type_misfire")
    ;

    TriggerTypeEnum(String title) {
        this.title = title;
    }

    private String title;
    public String getTitle() {
        return title;
    }
}
