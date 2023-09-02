package com.xiaohe.biz.model;

import java.io.Serializable;

/**
 * @author : 小何
 * @Description : 日志记录结果类
 * @date : 2023-09-02 12:19
 */
public class LogResult implements Serializable {
    private static final long serialVersionUID = 42L;

    /**
     * 本条日志的开始行数
     */
    private int fromLineNum;

    /**
     * 本条日志在日志文件中一共有多少行
     */
    private int toLineNum;

    /**
     * 日志内容
     */
    private String logContent;

    /**
     * 是否结束
     */
    private boolean isEnd;

    public LogResult() {
    }

    public LogResult(int fromLineNum, int toLineNum, String logContent, boolean isEnd) {
        this.fromLineNum = fromLineNum;
        this.toLineNum = toLineNum;
        this.logContent = logContent;
        this.isEnd = isEnd;
    }

    public int getFromLineNum() {
        return fromLineNum;
    }

    public void setFromLineNum(int fromLineNum) {
        this.fromLineNum = fromLineNum;
    }

    public int getToLineNum() {
        return toLineNum;
    }

    public void setToLineNum(int toLineNum) {
        this.toLineNum = toLineNum;
    }

    public String getLogContent() {
        return logContent;
    }

    public void setLogContent(String logContent) {
        this.logContent = logContent;
    }

    public boolean isEnd() {
        return isEnd;
    }

    public void setEnd(boolean end) {
        isEnd = end;
    }
}
