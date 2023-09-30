package com.xiaohe.core.biz.impl;

import com.xiaohe.core.biz.ExecutorBiz;
import com.xiaohe.core.log.XxlJobFileAppender;
import com.xiaohe.core.model.*;

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
        return null;
    }

    @Override
    public Result run(TriggerParam triggerParam) {
        return null;
    }

    @Override
    public Result kill(KillParam killParam) {
        return null;
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
}
