package com.xiaohe.admin.core.thread;

import com.xiaohe.admin.core.complete.XxlJobCompleter;
import com.xiaohe.admin.core.conf.ScheduleTaskAdminConfig;
import com.xiaohe.admin.core.model.XxlJobLog;
import com.xiaohe.admin.core.util.I18nUtil;
import com.xiaohe.core.model.HandlerCallbackParam;
import com.xiaohe.core.model.Result;
import com.xiaohe.core.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author : 小何
 * @Description :
 * 1. 对于执行器发送的执行结果回调做出具体的处理 (并没有接收，调度中心的HTTP服务器接收后调用 JobCompleteHelper 做出处理)
 *    这里的处理也只是把任务的执行信息写到数据库。毕竟之前数据库的log里只有log信息。

 * 2. 对于 调度成功，但是由于执行器宕机而无法对执行状态 (handle_code) 做出更改的这些任务，将它们的执行状态改为失败。
 *     为什么呢？因为这些任务可能需要重试，但是 handle_code 不为0无法重试。所以用 JobCompleteHelper 将状态改一下。
 * @date : 2023-10-13 21:52
 */
public class JobCompleteHelper {
    private static Logger logger = LoggerFactory.getLogger(JobCompleteHelper.class);

    private static JobCompleteHelper instance = new JobCompleteHelper();
    public static JobCompleteHelper getInstance() {
        return instance;
    }
    /**
     * 这个线程池处理执行器回调回来的任务执行结果
     */
    private ThreadPoolExecutor callbackThreadPool = null;

    /**
     * 这个线程扫描那些调度了但是因为执行器宕机了所以无法更新 handle_code 的任务，将handle_code更新了。
     */
    private Thread monitorThread;

    /**
     * 组件是否结束
     */
    private volatile boolean toStop = false;

    public void start() {
        // 先给线程池new一下
        initCallbackThreadPool();
        // 给线程赋上任务
        monitorThread = new Thread(() -> {
            // Scheduled Task刚启动的时候肯定不这么急着执行任务。先睡50ms，让其他线程池初始化完毕
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                if (!toStop) {
                    logger.error(e.getMessage(), e);
                }
            }
            // 开始循环任务
            while (!toStop) {
                // 找到10分钟内 调度了 && 没执行 && 执行器宕机 的log
                // 即: xxl_job_log.trigger_code = 200 && xxl_job.handle_code = 0 && xxl_job_registry.id = null
                Date losedTime = DateUtil.addMinutes(new Date(), -10);
                List<Long> lostJobIds = ScheduleTaskAdminConfig.getAdminConfig().getXxlJobLogMapper().findLostJobIds(losedTime);
                for (Long lostJobId : lostJobIds) {
                    XxlJobLog xxlJobLog = new XxlJobLog();
                    xxlJobLog.setId(lostJobId);
                    xxlJobLog.setHandleTime(new Date());
                    xxlJobLog.setHandlerCode(Result.FAIL_CODE);
                    xxlJobLog.setHandleMsg( I18nUtil.getString("joblog_lost_fail") );
                    XxlJobCompleter.updateHandleInfoAndFinish(xxlJobLog);
                }
                // 一分钟扫描一次
                try {
                    TimeUnit.SECONDS.sleep(60);
                } catch (InterruptedException e) {
                    if (!toStop) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.setName("Scheduled Task, admin JobLosedMonitorHelper");
        monitorThread.start();
        logger.info(">>>>>>>>>>>> Scheduled Task, JobCompleteHelper start success");
    }

    private void initCallbackThreadPool() {
        callbackThreadPool = new ThreadPoolExecutor(
                2,
                20,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(3000),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "Scheduled Task, admin JobLosedMonitorHelper-callbackThreadPool-" + r.hashCode());
                    }
                },
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        r.run();
                        logger.warn(">>>>>>>>>> Scheduled Task, callback too fast, match threadpool rejected handler(run now)");
                    }
                }
        );
    }

    /**
     * 停止线程和线程池
     */
    public void toStop() {
        toStop = true;
        callbackThreadPool.shutdownNow();
        monitorThread.interrupt();
        try {
            monitorThread.join();
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * 处理执行器发送来的执行结果回调，批量处理
     * @param handlerCallbackParams
     * @return
     */
    public Result callback(List<HandlerCallbackParam> handlerCallbackParams) {
        callbackThreadPool.execute(() -> {
            for (HandlerCallbackParam handlerCallbackParam : handlerCallbackParams) {
                Result callback = callback(handlerCallbackParam);
                logger.debug(">>>>>>>>>>> JobApiController.callback: {}, handleCallbackParam= {}, callbackResult= {}",
                        callback.getCode() == Result.SUCCESS_CODE ? "success" : "fail",
                        handlerCallbackParam,
                        callback
                );
            }
        });
        return Result.SUCCESS;
    }

    private Result callback(HandlerCallbackParam handlerCallbackParam) {
        XxlJobLog log = ScheduleTaskAdminConfig.getAdminConfig().getXxlJobLogMapper().loadById(handlerCallbackParam.getLogId());
        if (log == null) {
            return Result.error("log item not found");
        }
        // 如果执行状态大于0，说明不管成功还是失败总之这个任务执行过一次
        if (log.getHandlerCode() > 0) {
            return Result.error("log repeate callback.");
        }
        StringBuffer handleMsg = new StringBuffer();
        if (log.getHandleMsg() != null) {
            handleMsg.append(log.getHandleMsg()).append("<br>");
        }
        if (handlerCallbackParam.getHandleMsg() != null) {
            handleMsg.append(handlerCallbackParam.getHandleMsg()).append("<br>");
        }
        log.setHandleTime(new Date());
        log.setHandlerCode(handlerCallbackParam.getHandleCode());
        log.setHandleMsg(handleMsg.toString());
        // 更新数据库中的Log的执行信息
        XxlJobCompleter.updateHandleInfoAndFinish(log);
        return Result.SUCCESS;
    }

}
