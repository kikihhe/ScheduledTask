package com.xiaohe.biz.model;

import java.io.Serializable;

/**
 * @author : 小何
 * @Description : 调度中心向执行器发送的检测执行对应任务的线程是否空闲的pojo类
 * @date : 2023-09-23 12:14
 */
public class IdleBeatParam implements Serializable {
    private static final long SerialVersionUID = 42L;

    private int jobId;

    public IdleBeatParam() {
    }

    public IdleBeatParam(int jobId) {
        this.jobId = jobId;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }
}
