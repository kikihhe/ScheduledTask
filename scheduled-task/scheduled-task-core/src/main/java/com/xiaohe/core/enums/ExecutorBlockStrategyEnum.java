package com.xiaohe.core.enums;

/**
 * @author : 小何
 * @Description : 定时任务的阻塞策略
 * 一个任务执行频率太快，如果前面的一次正在执行，后面的一次就已经到了，那么该如何处理？
 * 1. 串行。将后面的任务放入队列等待执行
 * 2. 丢弃后面的任务
 * 3. 丢弃前面的任务
 * @date : 2023-10-09 19:41
 */
public enum ExecutorBlockStrategyEnum {
    SERIAL_EXECUTION("Serial execution"),
    DISCARD_LATER("Discard Later"),
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
        for (ExecutorBlockStrategyEnum blockEnum : ExecutorBlockStrategyEnum.values()) {
            if (blockEnum.getTitle().equals(name)) {
                return blockEnum;
            }
        }
        return defaultEnum;
    }
}
