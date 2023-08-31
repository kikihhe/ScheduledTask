package com.xiaohe.core.model;

import java.util.Date;

/**
 * @author : 小何
 * @Description : 日志类
 * @date : 2023-08-31 13:46
 */
public class ScheduledTaskLog {

    /**
     * 日志id
     */
    private long id;

    /**
     * 执行器组
     */
    private int jobGroup;

    /**
     * 本条日志记录的是关于哪个定时任务
     */
    private int jobId;

    /**
     * 执行 本次定时任务 的执行器IP
     */
    private String executorAddress;

    /**
     * 封装定时任务的 JobHandler 的名称
     */
    private String executorHandler;

    /**
     * 执行器参数
     */
    private String executorParam;

    /**
     * 执行器分片参数
     */
    private String executorShardingParam;

    /**
     * 任务失败重试次数
     */
    private int executorFailRetryCount;

    /**
     * 调度时间
     */
    private Date triggerTime;

    /**
     * 调度的响应码
     */
    private int triggerCode;


    /**
     * 调度任务信息
     */
    private String triggerMsg;

    /**
     * 执行时间
     */
    private Date handleTime;

    /**
     * 执行响应码
     */
    private int handlerCode;

    /**
     * 执行任务信息
     */
    private String handlerMsg;

    /**
     * 警报的状态码
     * 0 : 默认
     * 1 : 不需要报警
     * 2 : 报警成功
     * 3 : 报警失败
     */
    private int alarmStatus;


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(int jobGroup) {
        this.jobGroup = jobGroup;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public String getExecutorAddress() {
        return executorAddress;
    }

    public void setExecutorAddress(String executorAddress) {
        this.executorAddress = executorAddress;
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

    public String getExecutorShardingParam() {
        return executorShardingParam;
    }

    public void setExecutorShardingParam(String executorShardingParam) {
        this.executorShardingParam = executorShardingParam;
    }

    public int getExecutorFailRetryCount() {
        return executorFailRetryCount;
    }

    public void setExecutorFailRetryCount(int executorFailRetryCount) {
        this.executorFailRetryCount = executorFailRetryCount;
    }

    public Date getTriggerTime() {
        return triggerTime;
    }

    public void setTriggerTime(Date triggerTime) {
        this.triggerTime = triggerTime;
    }

    public int getTriggerCode() {
        return triggerCode;
    }

    public void setTriggerCode(int triggerCode) {
        this.triggerCode = triggerCode;
    }

    public String getTriggerMsg() {
        return triggerMsg;
    }

    public void setTriggerMsg(String triggerMsg) {
        this.triggerMsg = triggerMsg;
    }

    public Date getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(Date handleTime) {
        this.handleTime = handleTime;
    }

    public int getHandlerCode() {
        return handlerCode;
    }

    public void setHandlerCode(int handlerCode) {
        this.handlerCode = handlerCode;
    }

    public String getHandlerMsg() {
        return handlerMsg;
    }

    public void setHandlerMsg(String handlerMsg) {
        this.handlerMsg = handlerMsg;
    }

    public int getAlarmStatus() {
        return alarmStatus;
    }

    public void setAlarmStatus(int alarmStatus) {
        this.alarmStatus = alarmStatus;
    }
}
