package com.xiaohe.core.thread;

import com.xiaohe.core.context.XxlJobContext;
import com.xiaohe.core.context.XxlJobHelper;
import com.xiaohe.core.handler.IJobHandler;
import com.xiaohe.core.log.XxlJobFileAppender;
import com.xiaohe.core.model.HandlerCallbackParam;
import com.xiaohe.core.model.Result;
import com.xiaohe.core.model.TriggerParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

/**
 * @author : 小何
 * @Description : 执行任务的方法，与定时任务绑定
 * @date : 2023-09-30 21:02
 */
public class JobThread extends Thread {
    private static Logger logger = LoggerFactory.getLogger(JobThread.class);

    /**
     * 定时任务的id
     */
    private int jobId;

    /**
     * 此任务绑定的对象
     */
    private IJobHandler handler;

    /**
     * 内含需要被执行的定时任务(触发器参数)
     */
    private LinkedBlockingQueue<TriggerParam> triggerQueue;

    /**
     * 正在调度的任务的日志id集合(线程安全)
     */
    private Set<Long> triggerLogIdSet;

    /**
     * 该组件是否结束，如果这个组件结束，说明该线程正在负责的定时任务被取消了
     */
    private volatile boolean toStop = false;

    /**
     * 该任务被取消的原因
     */
    private String stopReason;

    /**
     * 该线程是否正在运行(正在运行任务，如果只是等待从队列中取任务不算运行中)
     */
    private boolean running = false;

    /**
     * 空转次数，达到一定次数便销毁该线程，连阻塞都不让它阻塞
     */
    private int idleTimes = 0;

    public JobThread(int jobId, IJobHandler handler) {
        this.jobId = jobId;
        this.handler = handler;
        this.triggerQueue = new LinkedBlockingQueue<>();
        this.triggerLogIdSet = Collections.synchronizedSet(new HashSet<>());
        this.setName("xxl-job, JobThread - " + jobId + " - " + System.currentTimeMillis());
    }

    /**
     * 调度参数放入调度队列
     *
     * @param triggerParam
     */
    public Result pushTriggerQueue(TriggerParam triggerParam) {
        // 如果日志id集合中包含此调度参数，说明可能调度
        if (triggerLogIdSet.contains(triggerParam.getLogId())) {
            logger.info(">>>>>>>>>>>> repeate trigger job, logId: {}", triggerParam.getLogId());
            return Result.error("repeate trigger job, logId:" + triggerParam.getLogId());
        }
        triggerLogIdSet.add(triggerParam.getLogId());
        triggerQueue.add(triggerParam);
        return Result.success();
    }

    /**
     * 判断该线程是否正在忙碌，正在运行任务、阻塞队列中有调度参数等待执行 代表忙碌
     */
    public boolean isRunningOrHasQueue() {
        return running || !triggerQueue.isEmpty();
    }

    @Override
    public void run() {
        // 执行初始化方法
        runInitMethod();
        // 开始执行
        while (!toStop) {
            // 刚进入循环不算执行，得到调度参数才算执行
            running = false;
            idleTimes += 1;
            TriggerParam triggerParam = null;
            try {
                triggerParam = triggerQueue.poll(3L, TimeUnit.SECONDS);
                if (triggerParam == null) {
                    // 如果阻塞3s没有调度参数，并且现在已经循环了30次，队列中还是没有数据，得，销毁线程
                    if (idleTimes > 30 && triggerQueue.isEmpty()) {
                        // TODO 调用XxlJobExecutor.removeJobThread() 去销毁线程
                    }
                } else {
                    // 调度参数不为空，将running改为true, 空转次数改为0
                    running = true;
                    idleTimes = 0;
                    triggerLogIdSet.remove(triggerParam.getLogId());
                    String logFileName = XxlJobFileAppender.makeLogFileName(new Date(triggerParam.getLogDateTime()), triggerParam.getLogId());
                    XxlJobContext xxlJobContext = new XxlJobContext(
                            triggerParam.getJobId(),
                            triggerParam.getExecutorParams(),
                            logFileName,
                            triggerParam.getBroadcastIndex(),
                            triggerParam.getBroadcastTotal()
                    );
                    XxlJobContext.setXxlJobContext(xxlJobContext);
                    // 记录日志，开始执行
                    XxlJobHelper.log("<br>----------- xxl-job job execute start -----------<br>----------- Param:" + xxlJobContext.getJobParam());
                    // 执行定时任务，根据是否设置超时时间来执行。
                    // 如果没有设置超时时间就直接执行。 如果设置了超时时间就开启子线程，使用 FutureTask 执行。
                    doExecute(triggerParam, xxlJobContext);

                    // 判断执行结果
                    if (XxlJobContext.getXxlJobContext().getHandleCode() <= 0) {
                        XxlJobHelper.handleFail("job handle result lost.");
                    } else {
                        // 如果执行结果大于0，不管成功还是失败就直接记录消息，等回调线程去回调。如果执行成功了handleMsg为空
                        String tempHandleMsg = XxlJobContext.getXxlJobContext().getHandleMsg();
                        tempHandleMsg = (tempHandleMsg != null && tempHandleMsg.length() > 50000) ? tempHandleMsg.substring(0, 50000).concat("...") : tempHandleMsg;
                        XxlJobContext.getXxlJobContext().setHandleMsg(tempHandleMsg);
                    }
                    // 回调的执行结果咱不管，现在的执行结果先记录日志
                    XxlJobHelper.log("<br>------------ xxl-job job execute end(finish)-------------<br>------------- Result: handleCode=" +
                            XxlJobContext.getXxlJobContext().getHandleCode() +
                            ", handleMsg = " +
                            XxlJobContext.getXxlJobContext().getHandleMsg()
                    );
                }
            } catch (Exception e) {
                if (toStop) {
                    // 如果线程停止了，记录停止原因、异常
                    XxlJobHelper.log("<br>----------- JobThread toStop, stopReason:" + stopReason);
                    StringWriter stringWriter = new StringWriter();
                    e.printStackTrace(new PrintWriter(stringWriter));
                    String errorMessage = stringWriter.toString();
                    XxlJobHelper.handleFail(errorMessage);
                    XxlJobHelper.log("<br>----------- JobThread Exception:" + errorMessage +
                            "<br>----------- xxl-job job execute end(error)");
                }
            } finally {
                // 在finally中执行将日志回调给调度中心的操作
                if (triggerParam != null) {
                    // 如果线程没有停止，不管任务执行成功还是失败，回调给调度中心即可。
                    // 如果线程被终止了，将stopReason发送给调度中心
                    if (!toStop) {
                        TriggerCallbackThread.pushCallback(new HandlerCallbackParam(
                                triggerParam.getLogId(),
                                triggerParam.getLogDateTime(),
                                XxlJobContext.getXxlJobContext().getHandleCode(),
                                XxlJobContext.getXxlJobContext().getHandleMsg()
                        ));
                    } else {
                        TriggerCallbackThread.pushCallback(new HandlerCallbackParam(
                                triggerParam.getLogId(),
                                triggerParam.getLogDateTime(),
                                XxlJobContext.HANDLE_CODE_FAIL,
                                stopReason + "[job running, killed]"
                        ));
                    }
                }
            }

        }
        // 退出while循环，最后将队列中的数据拿出来回调回去，告诉调度中心这些数据没有执行
        while (triggerQueue != null && !triggerQueue.isEmpty()) {
            TriggerParam triggerParam = triggerQueue.poll();
            if (triggerParam == null) {
                continue;
            }
            TriggerCallbackThread.pushCallback(new HandlerCallbackParam(
                    triggerParam.getLogId(),
                    triggerParam.getLogDateTime(),
                    XxlJobContext.HANDLE_CODE_FAIL,
                    stopReason + " [job not executed, in the job queue, killed.]"
            ));
        }
        // 执行销毁方法
        runDestroyMethod();
    }

    private void runInitMethod() {
        try {
            handler.init();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void runDestroyMethod() {
        try {
            handler.destroy();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 执行定时任务，根据是否设置超时时间来执行。
     * 如果设置了超时时间就开启子线程，使用 FutureTask 执行。
     * 如果没有设置超时时间就直接执行
     *
     * @param triggerParam
     * @param xxlJobContext
     */
    private void doExecute(TriggerParam triggerParam, XxlJobContext xxlJobContext) throws Exception {
        // 没有设置超时时间，直接执行，设置了超时时间，开启子线程执行，此线程监督子线程执行
        if (triggerParam.getExecutorTimeout() == 0) {
            handler.execute();
        } else {
            Thread futureThread = null;
            try {
                FutureTask<Boolean> futureTask = new FutureTask<>(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        XxlJobContext.setXxlJobContext(xxlJobContext);
                        handler.execute();
                        return true;
                    }
                });
                // 创建并启动线程，get结果，如果指定时间get不到，说明超时
                futureThread = new Thread(futureTask);
                futureThread.start();
                Boolean tempResult = futureTask.get(triggerParam.getExecutorTimeout(), TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                XxlJobHelper.log("<br>-------------- xxl-job job execute timeout");
                XxlJobHelper.log(e);
                // 将超时设置到 XxlJobContext 中
                XxlJobHelper.handleTimeout("job execute timeout");
            } finally {
                futureThread.interrupt();
            }
        }
    }

    public void toStop(String stopReason) {
        toStop = true;
        this.stopReason = stopReason;
    }


    public IJobHandler getHandler() {
        return handler;
    }


}
