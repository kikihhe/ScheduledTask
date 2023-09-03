package com.xiaohe.biz.impl;

import com.xiaohe.biz.ExecutorBiz;
import com.xiaohe.biz.model.IdleBeatParam;
import com.xiaohe.biz.model.LogParam;
import com.xiaohe.biz.model.Result;
import com.xiaohe.biz.model.TriggerParam;
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
     * @return
     */
    @Override
    public Result<String> beat() {
        return Result.SUCCESS;
    }

    /**
     * 忙碌检测，去看对应的线程是否在执行任务
     * @param idleBeatParam
     * @return
     */
    @Override
    public Result<String> idleBeat(IdleBeatParam idleBeatParam) {
        return null;
    }

    /**
     * 任务的调度
     * @param triggerParam
     * @return
     */
    @Override
    public Result<String> run(TriggerParam triggerParam) {
        return null;
    }

    /**
     * 调度中心查看对应执行器的日志
     * @param logParam
     * @return
     */
    @Override
    public Result<String> log(LogParam logParam) {
        return null;
    }
}
