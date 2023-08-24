package com.xiaohe.ScheduledTask.core.thread;

import com.xiaohe.ScheduledTask.core.biz.model.Result;
import com.xiaohe.ScheduledTask.core.biz.model.TriggerParam;
import com.xiaohe.ScheduledTask.core.handler.impl.MethodJobHandler;
import com.xiaohe.ScheduledTask.core.util.ObjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author : 小何
 * @Description : 指定线程执行指定的定时任务
 * @date : 2023-08-24 17:22
 */
public class JobThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(JobThread.class);
    /**
     * 现在执行的定时任务的id
     */
    private int jobId;

    /**
     * 现在执行的定时任务的handler
     */
    private MethodJobHandler methodJobHandler;

    /**
     * 此线程处理的任务被调用后就存储在队列中，线程从阻塞队列中取任务执行
     */
    private LinkedBlockingQueue<TriggerParam> triggerQueue;

    /**
     * 线程终止的标记
     */
    private volatile boolean toStop = false;

    public JobThread(int jobId, MethodJobHandler methodJobHandler) {
        this.jobId = jobId;
        this.methodJobHandler = methodJobHandler;
    }

    /**
     * 将调度参数放入队列
     * @param triggerParam 调度参数
     * @return
     */
    public Result<String> pushTriggerQueue(TriggerParam triggerParam) {
        triggerQueue.add(triggerParam);
        return Result.SUCCESS;
    }

    @Override
    public void run() {
        while (!toStop) {
            // 从阻塞队列中取调度参数, 如果3s内没有取到，就阻塞
            TriggerParam triggerParam = null;
            try {
                triggerParam = triggerQueue.poll(3L, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.debug("3s内没取到参数，线程阻塞");
            }
            if (ObjectUtil.isNotNull(triggerParam)) {
                try {
                    methodJobHandler.execute();
                } catch (Exception e) {
                    logger.debug("执行出错");
                }
            }

        }
    }
}
