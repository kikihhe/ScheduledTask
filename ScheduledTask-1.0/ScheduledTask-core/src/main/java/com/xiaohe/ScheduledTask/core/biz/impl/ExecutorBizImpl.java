package com.xiaohe.ScheduledTask.core.biz.impl;

import com.xiaohe.ScheduledTask.core.biz.ExecutorBiz;
import com.xiaohe.ScheduledTask.core.biz.model.IdleBeatParam;
import com.xiaohe.ScheduledTask.core.biz.model.Result;
import com.xiaohe.ScheduledTask.core.biz.model.TriggerParam;
import com.xiaohe.ScheduledTask.core.executor.ScheduledTaskExecutor;
import com.xiaohe.ScheduledTask.core.glue.GlueTypeEnum;
import com.xiaohe.ScheduledTask.core.handler.impl.MethodJobHandler;
import com.xiaohe.ScheduledTask.core.thread.JobThread;
import com.xiaohe.ScheduledTask.core.util.ObjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author : 小何
 * @Description : 调度中心发来信息(调度任务、心跳检测、忙碌检测)后，做出对应动作的实现类
 * @date : 2023-08-25 00:07
 */
public class ExecutorBizImpl implements ExecutorBiz {
    private static Logger logger = LoggerFactory.getLogger(ExecutorBizImpl.class);

    /**
     * 向调度中心发送心跳
     *
     * @return
     */
    @Override
    public Result beat() {
        return Result.SUCCESS;
    }

    /**
     * 检测对应定时任务线程是否正在忙碌
     *
     * @param idleBeatParam
     * @return
     */
    @Override
    public Result idleBeat(IdleBeatParam idleBeatParam) {
        return Result.SUCCESS;
    }

    /**
     * 调度对应的任务
     *
     * @param triggerParam
     * @return
     */
    @Override
    public Result run(TriggerParam triggerParam) {
        // 通过jobId从 jobThreadRepository中取出执行这个定时任务的线程
        JobThread jobThread = ScheduledTaskExecutor.loadJobThread(triggerParam.getJobId());
        // 如果jobThread为空，jobHandler必然为空
        MethodJobHandler jobHandler = jobThread == null ? null : jobThread.getMethodJobHandler();
        String removeOldReason = "";
        // 该任务的调度模式
        GlueTypeEnum glueType = GlueTypeEnum.match(triggerParam.getGlueType());
        // 如果为bean模式，直接通过name在 jobHandlerRepository 中再得到jobHandler
        // 让上面的jobHandler和这个newJobHandler比较
        if (GlueTypeEnum.BEAN == glueType) {
            // 通过name获得jobHandler
            MethodJobHandler newJobHandler = ScheduledTaskExecutor.loadJobHandler(triggerParam.getExecutorHandler());
            // 如果jobHandler != newJobHandler, 说明定时任务已经改变了
            // 为什么用 == 判断不用equals, 因为注册的时候用的同一个jobHandler注册的，如果任务没有改变，它俩地址肯定一样
            if (ObjectUtil.isNotNull(jobHandler) && jobHandler != newJobHandler) {
                // 定时任务变了，将旧的线程杀死，换成新的
                removeOldReason = "change jobhandler or glue type, and terminate the old job thread.";
                jobHandler = null;
                jobThread = null;
            }
            if (jobHandler == null) {
                // jobHandler为空，说明JobThread为空，这个任务是第一次被调度
                jobHandler = newJobHandler;
                if (jobHandler == null) {
                    // newJobHandler也为空，说明根据定时任务名字根本找不到对应的 JobHandler
                    return new Result<String>(Result.FAIL_CODE, "job handler [" + triggerParam.getExecutorHandler() + "] not found.");
                }
            }
        } else {
            // 没有指定调度模式，直接返回失败
            return new Result<String>(Result.FAIL_CODE, "glueType[" + triggerParam.getGlueType() + "] is not valid.");
        }
        if (ObjectUtil.isNull(jobThread)) {
            // 定时任务还没有执行过
            jobThread = ScheduledTaskExecutor.registJobThread(triggerParam.getJobId(), jobHandler);

        }
        // 将调度信息放入jobThread的队列中
        Result<String> pushResult = jobThread.pushTriggerQueue(triggerParam);
        return pushResult;
    }
}
