package com.xiaohe.admin.core.scheduler;

/**
 * @author : 小何
 * @Description : 定时任务的调度类型，默认为cron类型
 * @date : 2023-09-27 14:29
 */
public enum ScheduleTypeEnum {
    /**
     * 不使用任何类型
     */
    NONE("schedule_type_none"),

    /**
     * cron表达式类型
      */
    CRON("schedule_type_cron"),

    /**
     * 固定频率类型
      */
    FIX_RATE("schedule_type_fix_rate")


    ;

    private String title;

    ScheduleTypeEnum(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 匹配某一种策略
     * @param name
     * @param defaultEnum
     * @return
     */
    public static ScheduleTypeEnum match(String name, ScheduleTypeEnum defaultEnum) {
        for (ScheduleTypeEnum value : ScheduleTypeEnum.values()) {
            if (value.getTitle().equals(name)) {
                return value;
            }
        }
        return defaultEnum;
    }
}
