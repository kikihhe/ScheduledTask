package com.xiaohe.core.scheduler;

/**
 * @author : 小何
 * @Description : 定时任务的调度类型
 * @date : 2023-09-16 11:31
 */
public enum ScheduleTypeEnum {
    /**
     * 不使用任何类型
     */
    NONE("schedule_type_none"),
    /**
     * 一般都是 cron 表达式
     */
    CRON("schedule_type_acron"),

    /**
     * 按照固定频率
     */
    FIX_RATE("schedule_type_fix_rate")

    ;
    private String title;

    ScheduleTypeEnum(String title) {
        this.title = title;
    }

    /**
     * 根据名字匹配对应的调度类型，没有匹配上就使用传入的 defaultType
     * @param name
     * @param defaultType
     * @return
     */
    public static ScheduleTypeEnum match(String name, ScheduleTypeEnum defaultType) {
        for (ScheduleTypeEnum item : ScheduleTypeEnum.values()) {
            if(item.name().equals(name)) {
                return item;
            }
        }
        return defaultType;
    }

    public String getTitle() {
        return title;
    }
}

