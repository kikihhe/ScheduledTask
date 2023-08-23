package com.xiaohe.ScheduledTask.admin.core.model;

import java.util.Date;

/**
 * @author : 小何
 * @Description : 任务 model类
 * @date : 2023-08-22 16:07
 */
public class ScheduledTaskInfo {
    /**
     * 任务id
     */
    private int id;

    /**
     * 该任务的名称
     */
    private String executorHandler;

    /**
     * 最近一次的触发时间
     */
    private long triggerLastTime;

    /**
     * 下一次触发时间
     */
    private long triggerNextTime;


    /**
     * 任务所属的组(执行器)
     */
    private int jobGroup;

    /**
     * 该任务的描述
     */
    private String jobDesc;

    /**
     * 该任务的创建时间
     */
    private Date addTime;

    /**
     * 该任务的最新修改时间
     */
    private Date updateTime;

    /**
     * 该任务的负责人
     */
    private String author;

    /**
     * 该任务的报警邮件
     */
    private String alarmEmail;

    /**
     * 该任务的调度类型
     */
    private String scheduleType;

    /**
     * 该任务的cron表达式
     */
    private String scheduleConf;

    /**
     * 该任务的失败策略
     */
    private String misfireStrategy;

    /**
     * 该任务的路由策略
     */
    private String executorRouteStrategy;

    /**
     * 执行参数
     */
    private String executorParam;

    /**
     * 该任务的阻塞策略
     */
    private String executorBlockStrategy;

    /**
     * 执行的超时时间
     */
    private int executorTimeout;

    /**
     * 失败重试次数
     */
    private int executorFailRetryCount;

    /**
     * 该任务的运行类型
     */
    private String glueType;

    /**
     * glue的源码
     */
    private String glueSource;

    /**
     * glue的备注
     */
    private String glueRemark;

    /**
     * glue更新时间
     */
    private Date glueUpdatetime;

    /**
     * 子任务id
     */
    private String childJobId;

    /**
     * 该任务的触发状态，0:停止即删除 , 1:正常
     */
    private int triggerStatus;


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
}
