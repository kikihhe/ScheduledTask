package com.xiaohe.core.model;

import java.util.Date;

/**
 * @author : 小何
 * @Description : 定时任务
 * @date : 2023-08-31 12:13
 */
public class ScheduledTaskInfo {
    /**
     * 定时任务id
     */
    private int id;

    /**
     * 可以执行该任务的执行器组
     */
    private int jobGroup;

    /**
     * 任务描述
     */
    private String jobDesc;


    /**
     * 调度状态，
     * 0 : 停止
     * 1 : 运行
     */
    private int triggerStatus;

    /**
     * 上一次调度的时间
     */
    private long triggerLastTime;

    /**
     * 下一次调度的时间
     */
    private long triggerNextTime;

    /**
     * 定时任务创建时间
     */
    private Date addTime;

    /**
     * 定时任务修改时间
     */
    private Date updateTime;

    /**
     * 任务负责人
     */
    private String author;

    /**
     * 告警邮箱
     */
    private String alarmEmail;

    /**
     * 调度类型
     */
    private String scheduleType;

    /**
     * cron表达式
     */
    private String scheduleConf;

    /**
     * 定时任务的失败策略
     */
    private String misfireStrategy;

    /**
     * 定时任务的路由策略
     */
    private String executorRouteStrategy;

    /**
     * jobHandler的名称
     */
    private String executorHandler;

    /**
     * 执行器参数
     */
    private String executorParam;

    /**
     * 定时任务阻塞策略
     */
    private String executorBlockStrategy;

    /**
     * 执行超时时间
     */
    private int executorTimeout;

    /**
     * 执行失败的重试次数
     */
    private int executorFailRetryCount;

    /**
     * 定时任务的运行类型
     */
    private String glueType;

    /**
     * glue的源码
     */
    private String glueSource;

    /**
     * glue备注
     */
    private String glueRemark;

    /**
     * glue更新时间
     */
    private Date glueUpdatetime;

    private String executorShardingParam;

    /**
     * 子任务id
     */
    private String childJobId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(int jobGroup) {
        this.jobGroup = jobGroup;
    }

    public String getJobDesc() {
        return jobDesc;
    }

    public void setJobDesc(String jobDesc) {
        this.jobDesc = jobDesc;
    }

    public int getTriggerStatus() {
        return triggerStatus;
    }

    public void setTriggerStatus(int triggerStatus) {
        this.triggerStatus = triggerStatus;
    }

    public long getTriggerLastTime() {
        return triggerLastTime;
    }

    public void setTriggerLastTime(long triggerLastTime) {
        this.triggerLastTime = triggerLastTime;
    }

    public long getTriggerNextTime() {
        return triggerNextTime;
    }

    public void setTriggerNextTime(long triggerNextTime) {
        this.triggerNextTime = triggerNextTime;
    }

    public Date getAddTime() {
        return addTime;
    }

    public void setAddTime(Date addTime) {
        this.addTime = addTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAlarmEmail() {
        return alarmEmail;
    }

    public void setAlarmEmail(String alarmEmail) {
        this.alarmEmail = alarmEmail;
    }

    public String getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(String scheduleType) {
        this.scheduleType = scheduleType;
    }

    public String getScheduleConf() {
        return scheduleConf;
    }

    public void setScheduleConf(String scheduleConf) {
        this.scheduleConf = scheduleConf;
    }

    public String getMisfireStrategy() {
        return misfireStrategy;
    }

    public void setMisfireStrategy(String misfireStrategy) {
        this.misfireStrategy = misfireStrategy;
    }

    public String getExecutorRouteStrategy() {
        return executorRouteStrategy;
    }

    public void setExecutorRouteStrategy(String executorRouteStrategy) {
        this.executorRouteStrategy = executorRouteStrategy;
    }

    public String getExecutorHandler() {
        return executorHandler;
    }

    public void setExecutorHandler(String executorHandler) {
        this.executorHandler = executorHandler;
    }

    public String getExecutorParam() {
        return executorParam;
    }

    public void setExecutorParam(String executorParam) {
        this.executorParam = executorParam;
    }

    public String getExecutorBlockStrategy() {
        return executorBlockStrategy;
    }

    public void setExecutorBlockStrategy(String executorBlockStrategy) {
        this.executorBlockStrategy = executorBlockStrategy;
    }

    public int getExecutorTimeout() {
        return executorTimeout;
    }

    public void setExecutorTimeout(int executorTimeout) {
        this.executorTimeout = executorTimeout;
    }

    public int getExecutorFailRetryCount() {
        return executorFailRetryCount;
    }

    public void setExecutorFailRetryCount(int executorFailRetryCount) {
        this.executorFailRetryCount = executorFailRetryCount;
    }

    public String getGlueType() {
        return glueType;
    }

    public void setGlueType(String glueType) {
        this.glueType = glueType;
    }

    public String getGlueSource() {
        return glueSource;
    }

    public void setGlueSource(String glueSource) {
        this.glueSource = glueSource;
    }

    public String getGlueRemark() {
        return glueRemark;
    }

    public void setGlueRemark(String glueRemark) {
        this.glueRemark = glueRemark;
    }

    public Date getGlueUpdatetime() {
        return glueUpdatetime;
    }

    public void setGlueUpdatetime(Date glueUpdatetime) {
        this.glueUpdatetime = glueUpdatetime;
    }

    public String getChildJobId() {
        return childJobId;
    }

    public void setChildJobId(String childJobId) {
        this.childJobId = childJobId;
    }

    public String getExecutorShardingParam() {
        return executorShardingParam;
    }

    public void setExecutorShardingParam(String executorShardingParam) {
        this.executorShardingParam = executorShardingParam;
    }
}
