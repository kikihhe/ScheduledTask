package com.xiaohe.ScheduledTask.core.thread;

import com.xiaohe.ScheduledTask.core.biz.model.Result;
import com.xiaohe.ScheduledTask.core.biz.model.TriggerParam;
import com.xiaohe.ScheduledTask.core.executor.ScheduledTaskExecutor;
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
     * 线程终止的标记，默认未终止
     */
    private volatile boolean toStop = false;
    /**
     * 线程终止原因
     */
    private volatile String stopReason;
    /**
     * 线程是否真正的正在队列中取任务
     */
    private volatile boolean running = false;

    /**
     * 线程的空闲循环次数。达到指定次数该线程
     */
    private int idleTimes = 0;

    public JobThread(int jobId, MethodJobHandler methodJobHandler) {
        this.jobId = jobId;
        this.methodJobHandler = methodJobHandler;
        this.triggerQueue = new LinkedBlockingQueue<TriggerParam>();
        this.setName("ScheduledTask, JobThread-jobId - " + jobId + " - " + System.currentTimeMillis());
    }

    /**
     * 将调度参数放入队列
     *
     * @param triggerParam 调度参数
     * @return
     */
    public Result<String> pushTriggerQueue(TriggerParam triggerParam) {
        triggerQueue.add(triggerParam);
        return Result.SUCCESS;
    }

    /**
     * 线程是否正在从队列中取任务执行，或者队列中是否还有任务
     * @return
     */
    public boolean isRunningOrHasQueue() {
        return running || !triggerQueue.isEmpty();
    }

    @Override
    public void run() {
        // 先执行bean的初始化方法
        try {
            methodJobHandler.init();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        // 循环
        while (!toStop) {
            // 线程虽然在工作，但没有在队列中取任务
            running = false;
            idleTimes++;
            // 从阻塞队列中取调度参数, 如果3s内没有取到，就阻塞
            TriggerParam triggerParam = null;
            try {
                triggerParam = triggerQueue.poll(3L, TimeUnit.SECONDS);

                // 从队列中取到任务了，就执行。并且把running设置为true, 空转次数重置。
                if (ObjectUtil.isNotNull(triggerParam)) {
                    running = true;
                    idleTimes = 0;
                    try {
                        methodJobHandler.execute();
                    } catch (Exception e) {
                        logger.debug("执行出错");
                    }
                } else {
                    // 循环空转30次并且队列中没有需要调度的参数时，线程就可以休息了。
                    if (idleTimes >= 30 && triggerQueue.isEmpty()) {
                        ScheduledTaskExecutor.removeJobThread(jobId, "excutor idel times over limit.");

                    }
                }
            } catch (InterruptedException e) {
                // 如果线程终止了，打印信息
                if (toStop) {
                    logger.info("<br>----------- JobThread toStop, stopReason:" + stopReason);
                }
            }

        }
        // 退出死循环代表 toStop = true，即该线程要终结了。
        try {
            methodJobHandler.destroy();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 将该线程终止
     *
     * @param stopReason
     */
    public void toStop(String stopReason) {
        //把线程终止标记设为true
        this.toStop = true;
        this.stopReason = stopReason;
    }

    public MethodJobHandler getMethodJobHandler() {
        return methodJobHandler;
    }
}
