package com.xiaohe.biz.model;

import java.io.Serializable;

/**
 * @author : 小何
 * @Description : 调度中心向执行器发送的调度参数
 * @date : 2023-08-31 19:54
 */
public class TriggerParam implements Serializable {
    private static final long serialVersionUID = 42L;

    /**
     * 定时任务id
     */
    private int jobId;

    /**
     * JobHandler的名字
     */
    private String executorHandler;

    /**
     * 定时任务参数
     */
    private String executorParams;

    /**
     * 阻塞策略
     */
    private String executorBlockStrategy;

    /**
     * 超时时间
     */
    private int executorTimeout;

    /**
     * 日志id
     */
    private long lonId;

    /**
     * 记录日志的时间
     */
    private long logDateTime;

    /**
     * 运行模式
     */
    private String glueType;

    /**
     * 代码文本
     */
    private String glueSource;

    /**
     * glue的更新时间
     */
    private long glueUpdatetime;

    /**
     * 分片索引
     */
    private int broadcastIndex;

    /**
     * 分片总数
     */
    private int broadcastTotal;

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public String getExecutorHandler() {
        return executorHandler;
    }

    public void setExecutorHandler(String executorHandler) {
        this.executorHandler = executorHandler;
    }

    public String getExecutorParams() {
        return executorParams;
    }

    public void setExecutorParams(String executorParams) {
        this.executorParams = executorParams;
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

    public long getLonId() {
        return lonId;
    }

    public void setLonId(long lonId) {
        this.lonId = lonId;
    }

    public long getLogDateTime() {
        return logDateTime;
    }

    public void setLogDateTime(long logDateTime) {
        this.logDateTime = logDateTime;
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

    public long getGlueUpdatetime() {
        return glueUpdatetime;
    }

    public void setGlueUpdatetime(long glueUpdatetime) {
        this.glueUpdatetime = glueUpdatetime;
    }

    public int getBroadcastIndex() {
        return broadcastIndex;
    }

    public void setBroadcastIndex(int broadcastIndex) {
        this.broadcastIndex = broadcastIndex;
    }

    public int getBroadcastTotal() {
        return broadcastTotal;
    }

    public void setBroadcastTotal(int broadcastTotal) {
        this.broadcastTotal = broadcastTotal;
    }
}
