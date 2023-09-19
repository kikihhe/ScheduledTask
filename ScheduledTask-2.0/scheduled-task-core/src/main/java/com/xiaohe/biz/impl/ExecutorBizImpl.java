package com.xiaohe.biz.impl;

import com.xiaohe.biz.ExecutorBiz;
import com.xiaohe.biz.model.IdleBeatParam;
import com.xiaohe.biz.model.LogParam;
import com.xiaohe.biz.model.Result;
import com.xiaohe.biz.model.TriggerParam;
import com.xiaohe.executor.ScheduledTaskExecutor;
import com.xiaohe.thread.JobThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author : 小何
 * @Description : 执行器端，当接收到调度中心发来的消息后实现对应处理的类
 * @date : 2023-09-03 12:46
 */
public class ExecutorBizImpl implements ExecutorBiz {
    private static Logger logger = LoggerFactory.getLogger(ExecutorBizImpl.class);

    /**
     * 心跳检测方法，直接回复成功即可
     */
    @Override
    public Result<String> beat() {
        return Result.SUCCESS;
    }

    /**
     * 忙碌检测，去看对应的线程是否在执行任务
     *
     * @param idleBeatParam
     */
    @Override
    public Result<String> idleBeat(IdleBeatParam idleBeatParam) {
        return null;
    }

    /**
     * 任务的调度
     *
     * @param triggerParam
     */
    @Override
    public Result<String> run(TriggerParam triggerParam) {
        return null;
    }

    /**
     * 调度中心查看对应执行器的日志
     *
     * @param logParam
     */
    @Override
    public Result<String> log(LogParam logParam) {
        return null;
    }

    /**
     * 执行器kill掉一个任务
     *
     * @param killParam
     */
    @Override
    public Result<String> kill(TriggerParam killParam) {
        JobThread jobThread = ScheduledTaskExecutor.loadJobThread(killParam.getJobId());
        if (jobThread != null) {
            ScheduledTaskExecutor.removeJobThread(killParam.getJobId(), "scheduling center kill job.");
        }
        return Result.SUCCESS;
    }
}
