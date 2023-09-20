package com.xiaohe.enums;

/**
 * @author : 小何
 * @Description : 任务阻塞策略
 * @date : 2023-09-19 21:58
 */
public enum ExecutorBlockStrategyEnum {
    /**
     * 串行
     */
    SERIAL_EXECUTION("Serial exection"),
    /**
     * 丢弃新任务
     */
    DISCARD_LATER("Discard Later"),

    /**
     * 丢弃旧任务
      */
    COVER_EARLY("Cover Early")
    ;
    private String title;

    ExecutorBlockStrategyEnum(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public static ExecutorBlockStrategyEnum match(String name, ExecutorBlockStrategyEnum defaultEnum) {
        for (ExecutorBlockStrategyEnum value : ExecutorBlockStrategyEnum.values()) {
            if (value.title.equals(name)) {
                return value;
            }
        }
        return defaultEnum;
    }
}
