package com.xiaohe.core.context;

/**
 * @author : 小何
 * @Description : 定时任务上下文对象
 * @date : 2023-09-29 16:28
 */
public class XxlJobContext {
    /**
     * 执行状态码:成功
     */
    public static final int HANDLE_CODE_SUCCESS = 200;
    /**
     * 执行状态码:失败
     */
    public static final int HANDLE_CODE_FAIL = 500;
    /**
     * 执行状态码:超时
     */
    public static final int HANDLE_CODE_TIMEOUT = 502;


    /**
     * 定时任务id
     */
    private final long jobId;

    private final String jobParam;

    /**
     * 记录定时任务日志的日志路径
     */
    private final String jobLogFileName;

    private final int shardIndex;

    private final int shardTotal;

    private int handleCode;

    private String handleMsg;

    public XxlJobContext(long jobId, String jobParam, String jobLogFileName, int shardIndex, int shardTotal) {
        this.jobId = jobId;
        this.jobParam = jobParam;
        this.jobLogFileName = jobLogFileName;
        this.shardIndex = shardIndex;
        this.shardTotal = shardTotal;
        this.handleCode = HANDLE_CODE_SUCCESS;
    }

    private static InheritableThreadLocal<XxlJobContext> contextHolder = new InheritableThreadLocal<>();

    public static void setXxlJobContext(XxlJobContext xxlJobContext) {
        contextHolder.set(xxlJobContext);
    }

    public static XxlJobContext getXxlJobContext() {
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

    public int getShardTotal() {
        return shardTotal;
    }

    public int getHandleCode() {
        return handleCode;
    }

    public void setHandleCode(int handleCode) {
        this.handleCode = handleCode;
    }

    public String getHandleMsg() {
        return handleMsg;
    }

    public void setHandleMsg(String handleMsg) {
        this.handleMsg = handleMsg;
    }

    public int getShardIndex() {
        return shardIndex;
    }

}
