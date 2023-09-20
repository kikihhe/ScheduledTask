package com.xiaohe.biz.impl;

import com.xiaohe.biz.ExecutorBiz;
import com.xiaohe.biz.model.IdleBeatParam;
import com.xiaohe.biz.model.LogParam;
import com.xiaohe.biz.model.Result;
import com.xiaohe.biz.model.TriggerParam;
import com.xiaohe.enums.ExecutorBlockStrategyEnum;
import com.xiaohe.executor.ScheduledTaskExecutor;
import com.xiaohe.handler.IJobHandler;
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
        JobThread jobThread = ScheduledTaskExecutor.loadJobThread(idleBeatParam.getJobId());
        if (jobThread == null || jobThread.isRunningOrHasQueue()) {
            return Result.SUCCESS;
        }
        return Result.FAIL;
    }

    /**
     * 任务的调度
     *
     * @param triggerParam
     */
    @Override
    public Result<String> run(TriggerParam triggerParam) {
        JobThread jobThread = ScheduledTaskExecutor.loadJobThread(triggerParam.getJobId());
        // jobThread不为空，此任务并不是第一次执行
        IJobHandler jobHandler = jobThread == null ? null : jobThread.getJobHandler();
        String removeOldReason = "";
        IJobHandler newJobThread = ScheduledTaskExecutor.loadJobHandler(triggerParam.getExecutorHandler());
        // 如果 JobThread中的JobHandler 和ScheduledTaskExecutor中的JobThread不一样， 说明定时任务已经被改变了。
        // 怎么改变，用户在web界面改变了定时任务
        if (jobThread != null && jobHandler != newJobThread) {
            removeOldReason = "change jobHandler, and terminate the old job thread";
            jobThread = null;
            jobHandler = null;
        }
        // jobHandler为空的情况:
        // 1. JobThread为空，得到的jobHandler必然为空
        // 2. 因为 JobThread 与 ScheduledTaskExecutor 中的 JobHandler 不一样，因此将jobHandler置为空
        if (jobHandler == null) {
            jobHandler = newJobThread;
            if (newJobThread == null) {
                return new Result<String>(Result.FAIL_CODE, "job handler [" + triggerParam.getExecutorHandler() + "] not found");
            }
        }
        // 开始使用阻塞策略执行定时任务
        if (jobThread != null) {
            ExecutorBlockStrategyEnum blockStrategy = ExecutorBlockStrategyEnum.match(triggerParam.getExecutorBlockStrategy(), null);
            if (ExecutorBlockStrategyEnum.DISCARD_LATER == blockStrategy) {
                // 丢弃新任务
                if (jobThread.isRunningOrHasQueue()) {
                    return new Result<>(Result.FAIL_CODE, "block strategy effect: " + ExecutorBlockStrategyEnum.DISCARD_LATER.getTitle());
                }
            } else if (ExecutorBlockStrategyEnum.COVER_EARLY == blockStrategy) {
                // 丢弃老任务
                if (jobThread.isRunningOrHasQueue()) {
                    removeOldReason = "block strategy effect: " + ExecutorBlockStrategyEnum.COVER_EARLY.getTitle();
                    jobThread = null;
                }
            } else {
                // 串行执行，等待下面执行即可
            }
        }
        // 1. 本来jobThread就没有注册
        // 2. 刚刚因为阻塞策略(丢弃老任务)的原因将 jobThread置为空了。
        if (jobThread == null) {
            ScheduledTaskExecutor.registJobThread(triggerParam.getJobId(), jobHandler, removeOldReason);
        }
        return jobThread.pushTriggerQueue(triggerParam);
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
