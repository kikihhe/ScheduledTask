package com.xiaohe.core.model;

import java.util.Date;

/**
 * @author : 小何
 * @Description : 日志报告
 * @date : 2023-08-31 13:59
 */
public class ScheduledTaskLogReport {

    private int id;

    /**
     * 调度时间
     */
    private Date triggerDay;

    /**
     * 正在运行的个数
     */
    private int runningCount;

    /**
     * 成功个数
     */
    private int sucCount;


    /**
     * 失败个数
     */
    private int failCount;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getTriggerDay() {
        return triggerDay;
    }

    public void setTriggerDay(Date triggerDay) {
        this.triggerDay = triggerDay;
    }

    public int getRunningCount() {
        return runningCount;
    }

    public void setRunningCount(int runningCount) {
        this.runningCount = runningCount;
    }

    public int getSucCount() {
        return sucCount;
    }

    public void setSucCount(int sucCount) {
        this.sucCount = sucCount;
    }

    public int getFailCount() {
        return failCount;
    }

    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }
}
