package com.xiaohe.core.biz.impl;

import com.xiaohe.core.biz.ExecutorBiz;
import com.xiaohe.core.enums.ExecutorBlockStrategyEnum;
import com.xiaohe.core.executor.XxlJobExecutor;
import com.xiaohe.core.handler.IJobHandler;
import com.xiaohe.core.log.XxlJobFileAppender;
import com.xiaohe.core.model.*;
import com.xiaohe.core.thread.JobThread;

import java.util.Date;


/**
 * @author : 小何
 * @Description : 执行器接收到调度中心发送的消息后，做出具体处理/回应的类
 * @date : 2023-09-30 19:39
 */
public class ExecutorBizImpl implements ExecutorBiz {
    /**
     * 调度中心发送给执行器查看执行器是否还或者，那么执行器只要给调度中心发送个消息就行了.
     * @return
     */
    @Override
    public Result beat() {
        return Result.success();
    }

    /**
     * 调度中心想要查看某个任务对应的线程是否正在忙碌，执行器去查看
     * @param idleBeatParam
     * @return
     */
    @Override
    public Result idleBeat(IdleBeatParam idleBeatParam) {
        JobThread jobThread = XxlJobExecutor.loadJobThread(idleBeatParam.getJobId());
        if (jobThread != null && jobThread.isRunningOrHasQueue()) {
            return Result.error("job thread is running or has trigger queue");
        }
        return Result.SUCCESS;
    }

    @Override
    public Result kill(KillParam killParam) {
        JobThread jobThread = XxlJobExecutor.loadJobThread(killParam.getJobId());
        if (jobThread != null) {
            XxlJobExecutor.removeJobThread(killParam.getJobId(), "scheduling center kill job.");
            return Result.SUCCESS;
        }
        return Result.success("job thread already killed.");
    }

    /**
     * 调度中心想要查询日志，执行器就去读日志返回
     * @param logParam
     * @return
     */
    @Override
    public Result log(LogParam logParam) {
        String logFileName = XxlJobFileAppender.makeLogFileName(new Date(logParam.getLogDateTim()), logParam.getLogId());
        LogResult logResult = XxlJobFileAppender.readLog(logFileName, logParam.getFromLineNum());
        return Result.success(logResult);
    }

    @Override
    public Result run(TriggerParam triggerParam) {
        // 从JobThread中得到定时任务
        JobThread jobThread = XxlJobExecutor.loadJobThread(triggerParam.getJobId());
        IJobHandler jobHandler = jobThread == null ? null : jobThread.getHandler();
        String removeOldReason = null;
        // 查看此任务有没有合适的调度模式，如果没有，就返回调用失败的信息
        if (triggerParam.getGlueType() == null) {
            return Result.error("glueType[" + triggerParam.getGlueType() + "] is not valid");
        }
        // 只管bean模式，不管在线编辑任务的模式了
        IJobHandler newJobHandler = XxlJobExecutor.loadJobHandler(triggerParam.getExecutorHandler());
        // 如果 jobHandler 与 newJobHandler 不同，说明定时任务被改变了。
        if (jobThread != null && newJobHandler != jobHandler) {
            removeOldReason = "change jobhandler or glue type, and terminate the old job thread.";
            jobHandler = null;
            jobThread = null;
        }
        // 这里的 jobHandler 为空有两种情况:
        // 1. jobThread为空，(导致上面那个if没有走) 说明任务没有执行过，或者最近没有执行过导致 job thread被销毁了。
        // 2. 由于 新、老job handler不同导致 jobHandler置为空了(也就是走了上面那个if)
        // 以上两种不管是什么情况，我们都想要 jobHandler 是 XxlJobExecutor 里面那个Map中的job handler
        if (jobHandler == null) {
            jobHandler = newJobHandler;
            // 如果jobHandler还为空，说明newJobHandler都为空，也就是从 XxlJobExecutor 的 Map 中取出的job handler都为空，
            // 也就是这个任务连注册都没有注册过，直接不执行了，返回错误信息
            if (jobHandler == null) {
                return Result.error("job handler [" + triggerParam.getExecutorHandler() + "] not found.");
            }
        }
        // 走到这里，JobThread里的job handler真正等于 XxlJobExecutor中的JobHandler
        if (jobThread != null) {
            ExecutorBlockStrategyEnum blockStrategy = ExecutorBlockStrategyEnum.match(triggerParam.getExecutorBlockStrategy(), null);
            if (ExecutorBlockStrategyEnum.DISCARD_LATER == blockStrategy && jobThread.isRunningOrHasQueue()) {
                return Result.error("block strategy effect：" + ExecutorBlockStrategyEnum.DISCARD_LATER.getTitle());
            } else if (ExecutorBlockStrategyEnum.COVER_EARLY == blockStrategy && jobThread.isRunningOrHasQueue()) {
                removeOldReason = "block strategy effect：" + ExecutorBlockStrategyEnum.COVER_EARLY.getTitle();
                jobThread = null;
            } else {

            }
        }
        // jobThread为空有三种情况
        // 1. 说明任务没有执行过，或者最近没有执行过导致 job thread被销毁了
        // 2. 新老jobHandler不一样，导致 jobThread被销毁了。
        // 3. 阻塞策略是覆盖旧任务，导致 jobThread被销毁了
        // 不管怎样，都要创建一个新的jobThread
        if (jobThread == null) {
            jobThread = XxlJobExecutor.registJobThread(triggerParam.getJobId(), jobHandler, removeOldReason);
        }
        Result<String> pushResult = jobThread.pushTriggerQueue(triggerParam);
        return pushResult;
    }
}
