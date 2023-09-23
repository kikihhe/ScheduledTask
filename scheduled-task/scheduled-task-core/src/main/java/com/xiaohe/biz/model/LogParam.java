package com.xiaohe.biz.model;

import java.io.Serializable;

/**
 * @author : 小何
 * @Description : 日志参数
 * @date : 2023-09-23 12:28
 */
public class LogParam implements Serializable {
    private static final long serialVersionUID = 42L;

    /**
     * 日志id
     */
    private long logId;

    /**
     * 日志打印时间
     */
    private long logDateTim;

    /**
     * 日志开始的行数
     */
    private int fromLineNum;

    public LogParam() {
    }

    public LogParam(long logId, long logDateTim, int fromLineNum) {
        this.logId = logId;
        this.logDateTim = logDateTim;
        this.fromLineNum = fromLineNum;
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

    public int getFromLineNum() {
        return fromLineNum;
    }

    public void setFromLineNum(int fromLineNum) {
        this.fromLineNum = fromLineNum;
    }
}
