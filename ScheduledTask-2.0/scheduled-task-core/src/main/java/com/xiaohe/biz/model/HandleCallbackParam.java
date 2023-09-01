package com.xiaohe.biz.model;

import java.io.Serializable;

/**
 * @author : 小何
 * @Description : 执行器给调度中心发送的回调消息, 主要是日志消息
 * @date : 2023-09-01 18:22
 */
public class HandleCallbackParam implements Serializable {
    private static final long serialVersionUID = 42L;

    /**
     * 日志id
     */
    private long logId;

    /**
     * 日志记录时间
     */
    private long logDateTim;

    /**
     * 状态码
     */
    private int handleCode;

    /**
     * 调用信息
     */
    private String handleMsg;

    public HandleCallbackParam() {
    }

    public HandleCallbackParam(long logId, long logDateTim, int handleCode, String handleMsg) {
        this.logId = logId;
        this.logDateTim = logDateTim;
        this.handleCode = handleCode;
        this.handleMsg = handleMsg;
    }

    public long getLogId() {
        return logId;
    }

    public void setLogId(long logId) {
        this.logId = logId;
    }

    public long getLogDateTim() {
        return logDateTim;
    }

    public void setLogDateTim(long logDateTim) {
        this.logDateTim = logDateTim;
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

    @Override
    public String toString() {
        return "HandleCallbackParam{" +
                "logId=" + logId +
                ", logDateTim=" + logDateTim +
                ", handleCode=" + handleCode +
                ", handleMsg='" + handleMsg + '\'' +
                '}';
    }
}
