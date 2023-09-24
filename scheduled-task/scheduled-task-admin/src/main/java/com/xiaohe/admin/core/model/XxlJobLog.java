package com.xiaohe.admin.core.model;

import java.util.Date;

/**
 * @author : 小何
 * @Description : 日志有关的实体类
 * @date : 2023-09-23 12:34
 */
public class XxlJobLog {
    private long id;

    /**
     * 产生此条日志的执行器属于哪个执行器组
     */
    private int jobGroup;

    /**
     * 日志对应的任务id
     */
    private int jobId;

    /**
     * 产生此条日志的执行器IP
     */
    private String executorAddress;

    /**
     * 任务的 JobsHandler
     */
    private String executorHandler;

    /**
     * 执行参数
     */
    private String executorParam;

    /**
     * 执行器的分片参数
     */
    private String executorShardingParam;

    /**
     * 任务的失败重试次数
     */
    private int executorFailRetryCount;

    /**
     * 调度时间(不是执行时间)
     */
    private Date triggerTime;

    /**
     * 调度状态
     */
    private int triggerCode;

    /**
     * 调度详细信息
     */
    private String triggerMsg;

    /**
     * 执行时间
     */
    private Date handleTime;

    /**
     * 执行状态码
     */
    private int handlerCode;

    /**
     * 执行详细信息
     */
    private String handleMsg;

    /**
     * 警报的状态码
     * 0: 默认
     * 1: 无需报警
     * 2: 报警成功
     * 3: 报警失败
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

    public String getHandleMsg() {
        return handleMsg;
    }

    public void setHandleMsg(String handleMsg) {
        this.handleMsg = handleMsg;
    }

    public int getAlarmStatus() {
        return alarmStatus;
    }

    public void setAlarmStatus(int alarmStatus) {

        this.alarmStatus = alarmStatus;
    }

    public int getHandlerCode() {
        return handlerCode;
    }

    public void setHandlerCode(int handlerCode) {
        this.handlerCode = handlerCode;
    }
}
