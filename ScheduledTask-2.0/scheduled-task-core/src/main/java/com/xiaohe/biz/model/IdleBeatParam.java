package com.xiaohe.biz.model;

import java.io.Serializable;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-09-01 10:48
 */
public class IdleBeatParam implements Serializable {
    private static final long serialVersionUID = 42L;

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
