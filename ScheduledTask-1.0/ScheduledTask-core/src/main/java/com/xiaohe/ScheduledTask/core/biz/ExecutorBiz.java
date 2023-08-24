package com.xiaohe.ScheduledTask.core.biz;

import com.xiaohe.ScheduledTask.core.biz.model.IdleBeatParam;
import com.xiaohe.ScheduledTask.core.biz.model.Result;
import com.xiaohe.ScheduledTask.core.biz.model.TriggerParam;

/**
 * @author : 小何
 * @Description : 调度中心发来信息(调度任务、心跳检测、忙碌检测)后，做出对应动作的接口
 * @date : 2023-08-24 23:58
 */
public interface ExecutorBiz {
    /**
     * 向调度中心发送心跳
     * @return
     */
    public Result beat();

    /**
     * 检测对应定时任务线程是否正在忙碌
     * @param idleBeatParam
     * @return
     */
    public Result idleBeat(IdleBeatParam idleBeatParam);

    /**
     * 调度对应的任务
     * @param triggerParam
     * @return
     */
    public Result run(TriggerParam triggerParam);

}
