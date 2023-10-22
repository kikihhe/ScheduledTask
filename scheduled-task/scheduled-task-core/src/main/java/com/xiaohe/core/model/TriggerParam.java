package com.xiaohe.core.model;

import java.io.Serializable;

/**
 * @author : 小何
 * @Description : 调度中心给执行器发送的调度参数
 * @date : 2023-09-23 11:52
 */
public class TriggerParam implements Serializable {
    private static final long serialVersionUID = 42L;

    /**
     * 任务id
     */
    private int jobId;

    /**
     * 执行定时任务的方法的全限定名
     */
    private String executorHandler;

    /**
     * 定时任务的参数
     */
    private String executorParams;

    /**
     * 定时任务的阻塞策略                <br></br>
     * 当一个定时任务想要执行，但是负责该任务的线程正在工作时会使用这个阻塞策略判断
     */
    private String executorBlockStrategy;

    /**
     * 任务的过期时间          <br></br>
     * 如果开启了过期时间，执行任务时会额外创建一个新线程执行任务，
     * 之前的线程负责监督新线程执行任务时间是否超时
     */
    private int executorTimeout;

    /**
     * 该任务对应的日志的id
     */
    private long logId;

    /**
     * 打日志的时间
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
     * 代码文本更新时间
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

    public long getLogId() {
        return logId;
    }

    public void setLogId(long logId) {
        this.logId = logId;
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

    @Override
    public String toString() {
        return "TriggerParam{" +
                "jobId=" + jobId +
                ", executorHandler='" + executorHandler + '\'' +
                ", executorParams='" + executorParams + '\'' +
                ", executorBlockStrategy='" + executorBlockStrategy + '\'' +
                ", executorTimeout=" + executorTimeout +
                ", logId=" + logId +
                ", logDateTime=" + logDateTime +
                ", glueType='" + glueType + '\'' +
                ", glueSource='" + glueSource + '\'' +
                ", glueUpdatetime=" + glueUpdatetime +
                ", broadcastIndex=" + broadcastIndex +
                ", broadcastTotal=" + broadcastTotal +
                '}';
    }
}
