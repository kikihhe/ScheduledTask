package com.xiaohe.core.model;

import java.io.Serializable;
import java.util.Date;

/**
 * @author : 小何
 * @Description : 定时任务信息类
 * @date : 2023-09-21 17:04
 */
public class XxlJobInfo implements Serializable {
    private static final long serialVersionUID = 42L;

    /**
     * 定时任务id
     */
    private int id;

    /**
     * 执行该定时任务的执行器组的id
     */
    private int jobGroup;

    /**
     * 任务描述
     */
    private String jobDesc;

    /**
     * 任务添加时间
     */
    private Date addTime;
    /**
     * 修改时间
     */
    private Date updateTime;


    /**
     * 任务负责人
     */
    private String author;

    /**
     * 报警邮件
     */
    private String alarmEmail;

    /**
     * 调度类型
     */
    private String scheduleType;

    /**
     * 调度的cron表达式
     */
    private String scheduleConf;

    /**
     * 定时任务的失败策略            <br></br>
     * 当执行器宕机，或者任务执行失败导致定时任务的执行时间没有刷新，导致超过5s的调度周期时就需要使用失败策略
     */
    private String misfireStrategy;


    /**
     * 路由策略
     */
    private String executorRouteStrategy;

    /**
     * JobHandler的名称
     */
    private String executorHandler;

    /**
     * 定时任务执行时的参数
     */
    private String executorParam;

    /**
     * 定时任务的阻塞策略        <br></br>
     * 一个任务3s执行一次，但是执行一次需要耗时5s，那么就可能造成阻塞，就会使用到阻塞策略
     */
    private String executorBlockStrategy;

    /**
     * 执行超时时间，单位: s
     */
    private int executorTimeout;

    /**
     * 任务失败重试次数，有一个线程定时从数据库中扫描失败任务去执行
     */
    private int executorFailRetryCount;

    /**
     * 定时任务的运行类型
     */
    private String glueType;

    /**
     * glue模式时定时任务的源码
     */
    private String glueSource;

    /**
     * glue模式下的备注
     */
    private String glueRemark;

    /**
     * glue更新时间
     */
    private Date glueUpdatetime;

    /**
     * 子任务id        <br></br>
     * 父任务与子任务的关系: 父任务执行完毕就执行子任务，父任务执行失败就不执行子任务
     */
    private String childJobId;

    /**
     * 定时任务的触发状态，0:停止，1:运行
     */
    private int triggerStatus;

    /**
     * 该任务上一次触发时间
     */
    private long triggerLastTime;

    /**
     * 定时任务的下一次触发时间
     */
    private long triggerNextTime;

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

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public void setTriggerNextTime(long triggerNextTime) {

        this.triggerNextTime = triggerNextTime;
    }
}
