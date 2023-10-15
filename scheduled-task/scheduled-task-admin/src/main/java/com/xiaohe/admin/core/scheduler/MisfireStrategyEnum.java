package com.xiaohe.admin.core.scheduler;

/**
 * @author : 小何
 * @Description : 调度失败策略
 * @date : 2023-10-14 14:41
 */
public enum MisfireStrategyEnum {
    DO_NOTHING("misfire_strategy_do_nothing"),
    FIRE_ONCE_NOW("misfire_strategy_fire_once_now")
    ;
    private String title;

    MisfireStrategyEnum(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public static MisfireStrategyEnum match(String name, MisfireStrategyEnum defaultStrategy) {
        for (MisfireStrategyEnum value : MisfireStrategyEnum.values()) {
            if (value.title.equals(name)) {
                return value;
            }
        }
        return defaultStrategy;
    }
}
