package com.xiaohe.core.model;

import java.io.Serializable;

/**
 * @author : 小何
 * @Description : 执行器向调度中心发送的执行回调类
 * @date : 2023-09-23 12:25
 */
public class HandlerCallbackParam implements Serializable {
    private static final long serialVersionUID = 42L;

    /**
     * 日志id
     */
    private long logId;

    /**
     * 打印日志的时间
     */
    private long logDateTim;

    /**
     * 执行结果状态码
     */
    private int handleCode;

    /**
     * 执行信息
     */
    private String handleMsg;

    public HandlerCallbackParam() {
    }

    public HandlerCallbackParam(long logId, long logDateTim, int handleCode, String handleMsg) {
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
        return "HandlerCallbackParam{" +
                "logId=" + logId +
                ", logDateTim=" + logDateTim +
                ", handleCode=" + handleCode +
                ", handleMsg='" + handleMsg + '\'' +
                '}';
    }
}
