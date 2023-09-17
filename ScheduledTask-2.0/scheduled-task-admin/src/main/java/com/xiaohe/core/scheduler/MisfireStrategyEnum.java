package com.xiaohe.core.scheduler;

/**
 * @author : 小何
 * @Description : 执行器宕机(执行时间未刷新)后的失败策略
 * @date : 2023-09-16 12:17
 */
public enum MisfireStrategyEnum {
    /**
     * 失败后啥也不干
     */
    DO_NOTHING("misfire_strategy_do_nothing"),

    /**
     * 失败后立即执行一次
     */
    FIRE_ONCE_NOW("misfire_strategy_fire_once_now");

    private String title;

    MisfireStrategyEnum(String title) {
        this.title = title;
    }


    /**
     * 根据name匹配对应的策略，没有匹配上就是用传入的默认策略
     * @param name
     * @param defaultMisFireStrategy
     * @return
     */
    public static MisfireStrategyEnum match(String name, MisfireStrategyEnum defaultMisFireStrategy) {
        for (MisfireStrategyEnum strategyEnum : MisfireStrategyEnum.values()) {
            if (strategyEnum.name().equals(name)) {
                return strategyEnum;
            }
        }
        return defaultMisFireStrategy;
    }

    public String getTitle() {
        return title;
    }
}
