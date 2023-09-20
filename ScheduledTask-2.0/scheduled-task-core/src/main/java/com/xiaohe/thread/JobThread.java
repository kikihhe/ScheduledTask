package com.xiaohe.thread;

import com.xiaohe.biz.model.HandleCallbackParam;
import com.xiaohe.biz.model.Result;
import com.xiaohe.biz.model.TriggerParam;
import com.xiaohe.context.ScheduledTaskContext;
import com.xiaohe.context.ScheduledTaskHelper;
import com.xiaohe.executor.ScheduledTaskExecutor;
import com.xiaohe.handler.IJobHandler;
import com.xiaohe.log.ScheduledTaskFileAppender;
import javafx.scene.control.TableColumnBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;

/**
 * @author : 小何
 * @Description : 执行定时任务的线程
 * @date : 2023-09-14 16:27
 */
public class JobThread extends Thread {
    private static Logger logger = LoggerFactory.getLogger(JobThread.class);

    /**
     * 线程负责的任务的id
     */
    private int jobId;

    /**
     * 用于定时任务的执行
     */
    private IJobHandler jobHandler;

    /**
     * 阻塞队列，如果阻塞策略是串行执行，多余的任务可以放在队列中等候
     */
    private LinkedBlockingQueue<TriggerParam> triggerQueue;

    /**
     * 正在执行的任务，使用日志判断是否正在执行
     */
    private Set<Long> triggerLogIdSet;

    /**
     * 线程是否终止
     */
    private volatile boolean toStop = false;

    /**
     * 终止线程的原因
     */
    private String stopReason;

    /**
     * 线程是否正在执行任务。线程活着不一定是正在执行任务，也有可能是空转
     */
    private boolean running = false;

    /**
     * 线程空转次数，达到30次就将线程终止。      <br></br>
     * 空转: 循环在阻塞队列中拿数据，拿不到算一次
     */
    private int idleTimes = 0;

    public JobThread(int jobId, IJobHandler jobHandler) {
        this.jobId = jobId;
        this.jobHandler = jobHandler;
        this.triggerQueue = new LinkedBlockingQueue<>();
        this.triggerLogIdSet = new HashSet<>();
        this.setName("scheduled-task, JobThread-" + jobId + System.currentTimeMillis());
    }

    /**
     * 将触发器参数放入阻塞队列中
     *
     * @param triggerParam
     */
    public Result pushTriggerQueue(TriggerParam triggerParam) {
        if (triggerLogIdSet.contains(triggerParam.getLogId())) {
            return new Result(Result.FAIL_CODE, "repeate trigger job, logId: " + triggerParam.getLogId());
        }
        triggerLogIdSet.add(triggerParam.getLogId());
        triggerQueue.add(triggerParam);
        return Result.SUCCESS;
    }

    @Override
    public void run() {
        // 初始化方法和销毁方法只需要执行一次
        try {
            jobHandler.init();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        while (!toStop) {
            // 虽然在循环，但是没有执行任务
            running = false;
            idleTimes++;
            TriggerParam triggerParam = null;
            try {
                triggerParam = triggerQueue.poll(3L, TimeUnit.SECONDS);

                if (Objects.isNull(triggerParam)) {
                    // 如果为空说明队列中没有调度参数
                    if (idleTimes > 30 && triggerQueue.isEmpty()) {
                        ScheduledTaskExecutor.removeJobThread(jobId, "trigger queue is empty for 30 idle times");
                    }

                } else {
                    // 如果不为空就执行该任务
                    running = true;
                    idleTimes = 0;
                    // 因为定时任务要执行了，将它的日志id从集合中删除
                    triggerLogIdSet.remove(triggerParam.getLogId());
                    // 因为要记录日志，创建一个日志文件名
                    String logFileName = ScheduledTaskFileAppender.makeLogFileName(new Date(triggerParam.getLogDateTime()), triggerParam.getLogId());
                    // 创建一个上下文对象，存储到线程容器中
                    ScheduledTaskContext context = new ScheduledTaskContext(
                            triggerParam.getJobId(),
                            triggerParam.getExecutorParams(),
                            logFileName,
                            triggerParam.getBroadcastIndex(),
                            triggerParam.getBroadcastTotal()
                    );
                    ScheduledTaskContext.setScheduledTaskContext(context);
                    // 记录日志
                    ScheduledTaskHelper.log("-----------job execute start-----------<br>----------- Param:"
                            + context.getJobParam());
                    // 如果没有设置超时时间，直接执行
                    if (triggerParam.getExecutorTimeout() <= 0) {
                        // 通过反射执行任务
                        jobHandler.executor();
                    } else {
                        // 如果设置了超时时间，创建子线程执行
                        Thread thread = null;
                        FutureTask<Boolean> futureTask = new FutureTask<>(() -> {
                            // 让子线程可以访问到context
                            ScheduledTaskContext.setScheduledTaskContext(context);
                            jobHandler.executor();
                            return true;
                        });
                        try {
                            thread = new Thread(futureTask);
                            thread.start();
                            futureTask.get(triggerParam.getExecutorTimeout(), TimeUnit.SECONDS);
                        } catch (TimeoutException e) {
                            // 记录日志
                            ScheduledTaskHelper.log("<br>--------- job execute timeout");
                            ScheduledTaskHelper.log(e);
                            ScheduledTaskHelper.handleTimeout("job execute timeout");
                        } finally {
                            // 不管有没有超时，都终止线程
                            thread.interrupt();
                        }
                    }

                    // 从上下文中获取任务执行的结果
                    if (ScheduledTaskContext.getScheduledTaskContext().getHandleCode() <= 0) {
                        ScheduledTaskHelper.handleFail("job handle result lost");
                    } else {
                        // 把执行结果拿出来，如果太长了截断一下
                        String handleMsg = ScheduledTaskContext.getScheduledTaskContext().getHandleMsg();

                        handleMsg = (handleMsg != null && handleMsg.length() > 50000) ?
                                handleMsg.substring(0, 50000).concat("...") :
                                handleMsg;
                        ScheduledTaskContext.getScheduledTaskContext().setHandleMsg(handleMsg);
                    }
                    // 不管成功还是失败，都要把结果记录到日志中
                    ScheduledTaskHelper.log("<br>----------- scheduled-task job execute end(finish) -----------" +
                            "<br>----------- Result: handleCode=" +
                            ScheduledTaskContext.getScheduledTaskContext().getHandleCode() +
                            ", handleMsg = " +
                            ScheduledTaskContext.getScheduledTaskContext().getHandleMsg());

                }
            } catch (Exception e) {
                // 如果线程停止了，记录日志
                if (toStop) {
                    ScheduledTaskHelper.log("<br>----------- JobThread toStop, stopReason:" + stopReason);
                    // 记录异常日志
                    StringWriter stringWriter = new StringWriter();
                    e.printStackTrace(new PrintWriter(stringWriter));
                    String errorMsg = stringWriter.toString();
                    ScheduledTaskHelper.handleFail(errorMsg);
                    ScheduledTaskHelper.log("<br>----------- JobThread Exception:" + errorMsg + "<br>----------- scheduled-task job execute end(error) -----------");
                }
            } finally {
                // 执行此次任务执行的日志的回调
                if (triggerParam != null) {
                    if (!toStop) {
                        TriggerCallbackThread.pushCallBack(new HandleCallbackParam(
                                triggerParam.getLogId(),
                                triggerParam.getLogDateTime(),
                                ScheduledTaskContext.HANDLE_CODE_FAIL,
                                stopReason + "[job running, killed]"
                        ));
                    }
                }
            }


        }
        // 退出了while循环代表 toStop被设置为true, 将队列中没有执行的任务通知给调度中心
        while (triggerQueue != null && !triggerQueue.isEmpty()) {
            TriggerParam triggerParam = triggerQueue.poll();
            if (triggerParam != null) {
                TriggerCallbackThread.pushCallBack(new HandleCallbackParam(
                        triggerParam.getLogId(),
                        triggerParam.getLogDateTime(),
                        ScheduledTaskContext.HANDLE_CODE_FAIL,
                        stopReason + "[job not executed, in the job queue, killed.]"
                ));

            }
        }


        // 执行销毁方法
        try {
            jobHandler.destroy();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        logger.info(getName() + " stoped at" + System.currentTimeMillis());


    }

    /**
     * 判断线程是否正在执行任务、是否将要执行任务
     */
    public boolean isRunningOrHasQueue() {
        return running || !triggerQueue.isEmpty();
    }

    public IJobHandler getJobHandler() {
        return jobHandler;
    }

    /**
     * 停止该线程，一般用于删除定时任务、停止定时任务框架
     *
     * @param stopReason
     */
    public void toStop(String stopReason) {
        this.toStop = true;
        this.stopReason = stopReason;
    }
}
