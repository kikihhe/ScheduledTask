package com.xiaohe.biz.model;

import java.io.Serializable;

/**
 * @author : 小何
 * @Description : 调度中心发送给执行器想要删除某个任务
 * @date : 2023-09-23 12:47
 */
public class KillParam implements Serializable {
    private static final long seralVersionUID = 42L;

    /**
     * 任务id
     */
    private int jobId;



    public KillParam() {
    }

    public KillParam(int jobId) {
        this.jobId = jobId;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }
}
