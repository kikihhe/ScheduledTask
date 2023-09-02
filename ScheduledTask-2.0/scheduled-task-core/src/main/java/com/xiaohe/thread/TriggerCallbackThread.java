package com.xiaohe.thread;

import com.xiaohe.biz.AdminBiz;
import com.xiaohe.biz.model.HandleCallbackParam;
import com.xiaohe.biz.model.Result;
import com.xiaohe.context.ScheduledTaskContext;
import com.xiaohe.context.ScheduledTaskHelper;
import com.xiaohe.executor.ScheduledTaskExecutor;
import com.xiaohe.log.ScheduledTaskFileAppender;
import com.xiaohe.util.FileUtil;
import com.xiaohe.util.JdkSerializeTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author : 小何
 * @Description : 执行结果回调线程
 * @date : 2023-09-02 10:35
 */
public class TriggerCallbackThread {
    private static Logger logger = LoggerFactory.getLogger(TriggerCallbackThread.class);

    private static TriggerCallbackThread instance = new TriggerCallbackThread();

    public static TriggerCallbackThread getInstance() {
        return instance;
    }

    /**
     * 阻塞队列，所有需要回调的 HandleCallbackParam 都放在此处，回调线程在此队列中取
     */
    private LinkedBlockingQueue<HandleCallbackParam> callbackQueue = new LinkedBlockingQueue<>();

    /**
     * 回调线程，就是这个线程在阻塞队列中取 HandleCallbackParam ，用http发送给调度中心
     */
    private Thread triggerCallbackThread;

    /**
     * 重试线程，回调失败后交给重试线程重试
     */
    private Thread triggerRetryCallbackThread;


    /**
     * 回调线程和重试线程是否停止工作
     */
    private volatile boolean toStop = false;

    /**
     * 回调失败的数据需要记录日志，这个路径就是该日志的路径
     */
    private static String failCallbackFilePath = ScheduledTaskFileAppender.getLogPath().concat(File.separator).concat("callbacklog").concat(File.separator);

    /**
     * 设置回调失败日志存储的文件名
     */
    private static String failCallbackFileName = failCallbackFilePath.concat("scheduled-task-callback-{x}").concat(".log");


    /**
     * 将 HandleCallbackParam 放入队列中
     *
     * @param callback
     */
    public static void pushCallBack(HandleCallbackParam callback) {
        getInstance().callbackQueue.add(callback);
        logger.debug(">>>>>>>>>>> scheduled-task, push callback request, logId:{}", callback.getLogId());
    }

    /**
     * 启动两个线程
     */
    public void start() {
        // 如果执行器无法与调度中心通信，直接结束
        if (ScheduledTaskExecutor.getAdminBizList().isEmpty()) {
            logger.warn(">>>>>>>>>>> scheduled-task, executor callback config fail, adminAddresses is null.");
            return;
        }

        // 给线程任务，并让他们启动
        triggerCallbackThread = new Thread(() -> {
            while (!toStop) {
                // 批量回调
                try {
                    if (!getInstance().callbackQueue.isEmpty()) {
                        List<HandleCallbackParam> callbackParamList = new ArrayList<>();
                        getInstance().callbackQueue.drainTo(callbackParamList);
                        doCallback(callbackParamList);
                    }
                } catch (Exception e) {
                    if (!toStop) {
                        logger.error(e.getMessage(), e);
                    }
                }

            }
            // 此线程结束后，最后一次将阻塞队列中全部数据取出回调
            try {
                if (!getInstance().callbackQueue.isEmpty()) {
                    List<HandleCallbackParam> callbackParamList = new ArrayList<>();
                    getInstance().callbackQueue.drainTo(callbackParamList);
                    doCallback(callbackParamList);
                }
            } catch (Exception e) {
                if (!toStop) {
                    logger.error(e.getMessage(), e);
                }
            }
            logger.info(">>>>>>>>>>> scheduled-task, executor callback thread destroy.");

        });

        // 重试线程的任务: 从失败回调文件中取出回调失败的数据重试回调, 每隔30s重新回调一次
        triggerRetryCallbackThread = new Thread(() -> {
            while (!toStop) {
                try {
                    retryFailCallbackFile();
                } catch (Exception e) {
                    if (!toStop) {
                        logger.error(e.getMessage(), e);
                    }
                }
                try {
                    TimeUnit.SECONDS.sleep(30);
                } catch (InterruptedException e) {
                    if (!toStop) {
                        logger.error(e.getMessage(), e);
                    }
                }

            }
            logger.info(">>>>>>>>>>> scheduled-task, executor retry callback thread destroy.");
        });

        triggerCallbackThread.setDaemon(true);
        triggerCallbackThread.setName("scheduled-task, executor TriggerCallbackThread");
        triggerCallbackThread.start();

        triggerRetryCallbackThread.setDaemon(true);
        triggerRetryCallbackThread.start();
    }

    /**
     * 具体的回调方法
     * @param callbackParamList
     */
    private void doCallback(List<HandleCallbackParam> callbackParamList) {
        boolean callbackRet = false;
        for (AdminBiz adminBiz : ScheduledTaskExecutor.getAdminBizList()) {
            try {
                // 进行回调, 回调成功或失败或异常 都要记录日志。只需要一个调度中心成功就行了。
                Result<String> callbackResult = adminBiz.callback(callbackParamList);
                if (callbackResult != null && Result.SUCCESS_CODE == callbackResult.getCode()) {
                    callbackLog(callbackParamList, "<br>----------- scheduled-task job callback finish.");
                    callbackRet = true;
                    break;
                } else {
                    callbackLog(callbackParamList, "<br>----------- scheduled-task job callback fail, callbackResult:" + callbackResult);
                }
            } catch (Exception e) {
                callbackLog(callbackParamList, "<br>----------- scheduled-task job callback error, errorMsg:" + e.getMessage());
            }

            // 如果尝试了所有调度中心还是失败的话，就需要进行重试，将回调失败的数据存储到本地文件中，方便重试线程去重试
            if (!callbackRet) {
                appendFailCallbackFile(callbackParamList);
            }
        }
    }

    /**
     * 回调过程中记录日志
     * @param callbackParamList
     * @param logContent 成功还是失败
     */
    private void callbackLog(List<HandleCallbackParam> callbackParamList, String logContent) {
        for (HandleCallbackParam callbackParam : callbackParamList) {
            // 将信息存储到定时任务的本地文件中
            String logFileName = ScheduledTaskFileAppender.makeLogFileName(new Date(callbackParam.getLogDateTim()), callbackParam.getLogId());
            // 设置上下文对象, 将上下文对象放到线程私有容器中
            ScheduledTaskContext.setScheduledTaskContext(new ScheduledTaskContext(
                    -1,
                    null,
                    logFileName,
                    -1,
                    -1
            ));
            ScheduledTaskHelper.log(logContent);
        }
    }

    /**
     * 部分数据回调失败，存入专门的文件中。
     * @param callbackParamList
     */
    private void appendFailCallbackFile(List<HandleCallbackParam> callbackParamList) {
        if (CollectionUtils.isEmpty(callbackParamList)) {
            return;
        }
        byte[] callbackParamList_bytes = JdkSerializeTool.serialize(callbackParamList);
        // 得到文件名
        File callbackLogFile = new File(failCallbackFileName.replace("{x}", String.valueOf(System.currentTimeMillis())));
        if (callbackLogFile.exists()) {
            for (int i = 0; i < 100; i++) {
                callbackLogFile = new File(failCallbackFileName.replace("{x}", String.valueOf(System.currentTimeMillis()).concat("-").concat(String.valueOf(i))));
                if (!callbackLogFile.exists()) {
                    break;
                }
            }
        }
        FileUtil.writeFileContent(callbackLogFile, callbackParamList_bytes);
    }

    /**
     * 将回调失败日志文件中的内容读出来(然后删掉文件)，重新回调
     */
    private void retryFailCallbackFile() {
        File callbackLogPath = new File(failCallbackFilePath);
        if (!callbackLogPath.exists()) {
            return;
        }
        if (callbackLogPath.isFile()) {
            callbackLogPath.delete();
        }
        if (!callbackLogPath.isDirectory() && callbackLogPath.list() != null && callbackLogPath.list().length > 0) {
            return;
        }
        // 遍历目录下的所有日志文件
        for (File callbackLogFile : callbackLogPath.listFiles()) {
            byte[] callbackParamList_bytes = FileUtil.readFileContent(callbackLogFile);
            // 如果没有数据就删掉
            if (callbackParamList_bytes == null || callbackParamList_bytes.length < 1) {
                callbackLogFile.delete();
                continue;
            }
            // 反序列化一下
            List<HandleCallbackParam> callbackParamList = (List<HandleCallbackParam>) JdkSerializeTool.deserialize(callbackParamList_bytes, List.class);
            // 拿到数据了，删除文件
            callbackLogFile.delete();
            // 重新回调一下
            doCallback(callbackParamList);
        }
    }

    /**
     * 停止 回调线程 和 回调重试线程
     */
    private void toStop() {
        toStop = true;
        // 如果这两个线程不为空，让他们执行完最后的任务再结束
        if (triggerCallbackThread != null) {
            triggerCallbackThread.interrupt();
            try {
                triggerCallbackThread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
        if (triggerRetryCallbackThread != null) {
            triggerRetryCallbackThread.interrupt();
            try {
                triggerRetryCallbackThread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }


}
