package com.xiaohe.admin.core.model;

import java.util.Date;

/**
 * @author : 小何
 * @Description : 这个类对应的数据库 用于统计每日执行的任务完成情况
 * @date : 2023-10-15 17:48
 */
public class XxlJobLogReport {
    private int id;
    /**
     * 调度日期
     */
    private Date triggerDay;

    /**
     * 正在运行的任务
     */
    private int runningCount;

    /**
     * 运行成功的任务
     */
    private int sucCount;

    /**
     * 运行失败的任务
     */
    private int failCount;

    public XxlJobLogReport() {
    }

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

    public XxlJobLogReport(int id, Date triggerDay, int runningCount, int sucCount, int failCount) {
        this.id = id;
        this.triggerDay = triggerDay;
        this.runningCount = runningCount;
        this.sucCount = sucCount;
        this.failCount = failCount;
    }
}
