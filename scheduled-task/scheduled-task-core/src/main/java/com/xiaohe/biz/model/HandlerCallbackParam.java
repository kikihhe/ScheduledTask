package com.xiaohe.biz.model;

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
}
