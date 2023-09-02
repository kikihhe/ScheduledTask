package com.xiaohe.context;

import io.netty.handler.codec.http.HttpServerCodec;

/**
 * @author : 小何
 * @Description : 定时任务上下文
 * @date : 2023-09-02 12:11
 */
public class ScheduledTaskContext {
    /**
     * 执行成功状态码
     */
    public static final int HANDLE_CODE_SUCCESS = 200;

    /**
     * 执行失败状态码
     */
    public static final int HANDLE_CODE_FAIL = 500;

    /**
     * 超时状态码
     */
    public static final int HANDLE_CODE_TIMEOUT = 502;

    private final long jobId;

    private final String jobParam;

    private final String jobLogFileName;

    private final int shardIndex;

    private final int shardTotal;

    private int handleCode;

    private String handleMsg;

    public ScheduledTaskContext(long jobId, String jobParam, String jobLogFileName, int shardIndex, int shardTotal) {
        this.jobId = jobId;
        this.jobParam = jobParam;
        this.jobLogFileName = jobLogFileName;
        this.shardIndex = shardIndex;
        this.shardTotal = shardTotal;
        // 默认成功
        this.handleCode = HANDLE_CODE_SUCCESS;
    }

    /**
     * 线程私有的 ScheduledTaskContext 容器，每个线程对应一个ScheduledTaskContext
     */
    private static InheritableThreadLocal<ScheduledTaskContext> contextHolder = new InheritableThreadLocal<>();

    public static void setScheduledTaskContext(ScheduledTaskContext context) {
        contextHolder.set(context);
    }
    public static ScheduledTaskContext getScheduledTaskContext() {
        return contextHolder.get();
    }


    public long getJobId() {
        return jobId;
    }

    public String getJobParam() {
        return jobParam;
    }

    public String getJobLogFileName() {
        return jobLogFileName;
    }

    public int getShardIndex() {
        return shardIndex;
    }

    public int getShardTotal() {
        return shardTotal;
    }

    public void setHandleCode(int handleCode) {
        this.handleCode = handleCode;
    }

    public int getHandleCode() {
        return handleCode;
    }

    public void setHandleMsg(String handleMsg) {
        this.handleMsg = handleMsg;
    }

    public String getHandleMsg() {
        return handleMsg;
    }
}
