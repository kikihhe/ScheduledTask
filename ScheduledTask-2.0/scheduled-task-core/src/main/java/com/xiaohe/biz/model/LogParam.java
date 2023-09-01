package com.xiaohe.biz.model;

import java.io.Serializable;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-09-01 10:53
 */
public class LogParam implements Serializable {
    private static final long serialVersionUID = 42L;

    public LogParam() {
    }

    /**
     * 打印日志的时间
     */
    private long logDateTim;

    /**
     * 该条日志的id
     */
    private long logId;

    /**
     * 该条日志的起始行数
     */
    private int fromLineNum;

    public LogParam(long logDateTim, long logId, int fromLineNum) {
        this.logDateTim = logDateTim;
        this.logId = logId;
        this.fromLineNum = fromLineNum;
    }

    public long getLogDateTim() {
        return logDateTim;
    }

    public void setLogDateTim(long logDateTim) {
        this.logDateTim = logDateTim;
    }

    public long getLogId() {
        return logId;
    }

    public void setLogId(long logId) {
        this.logId = logId;
    }

    public int getFromLineNum() {
        return fromLineNum;
    }

    public void setFromLineNum(int fromLineNum) {
        this.fromLineNum = fromLineNum;
    }
}
