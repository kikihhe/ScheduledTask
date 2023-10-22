package com.xiaohe.admin.core.scheduler;

import com.xiaohe.admin.core.util.I18nUtil;

/**
 * @author : 小何
 * @Description : 调度失败策略
 * @date : 2023-10-14 14:41
 */
public enum MisfireStrategyEnum {

    //默认什么也不做
    DO_NOTHING(I18nUtil.getString("misfire_strategy_do_nothing")),

    //失败后重试一次
    FIRE_ONCE_NOW(I18nUtil.getString("misfire_strategy_fire_once_now"));
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
