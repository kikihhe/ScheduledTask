package com.xiaohe.ScheduledTask.core.biz.model;

import java.io.Serializable;

/**
 * @author : 小何
 * @Description : 调度中心使用忙碌策略时发送的信息对应的实体类
 * @date : 2023-08-24 23:56
 */
public class IdleBeatParam implements Serializable {
    private static final long serialVersionUID = 42L;

    /**
     * 定时任务id
     */
    private int jobId;

    public IdleBeatParam() {
    }

    public IdleBeatParam(int jobId) {
        this.jobId = jobId;
    }
}
