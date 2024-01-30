package com.xiaohe.core.thread;

import com.xiaohe.core.biz.AdminBiz;
import com.xiaohe.core.context.XxlJobContext;
import com.xiaohe.core.context.XxlJobHelper;
import com.xiaohe.core.executor.XxlJobExecutor;
import com.xiaohe.core.log.XxlJobFileAppender;
import com.xiaohe.core.model.HandlerCallbackParam;
import com.xiaohe.core.model.Result;
import com.xiaohe.core.util.CollectionUtil;
import com.xiaohe.core.util.FileUtil;
import com.xiaohe.core.util.JdkSerializeTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author : 小何
 * @Description : 将任务执行结果回调给调度中心
 * @date : 2023-09-30 14:24
 */
public class TriggerCallbackThread {
    private static Logger logger = LoggerFactory.getLogger(TriggerCallbackThread.class);

    private static TriggerCallbackThread instance = new TriggerCallbackThread();

    public static TriggerCallbackThread getInstance() {
        return instance;
    }

    /**
     * 要被回调的信息放在此处
     */
    private LinkedBlockingQueue<HandlerCallbackParam> callbackQueue = new LinkedBlockingQueue<>();

    /**
     * 提供外部调用，将回调信息放入回调队列中等待回调
     *
     * @param callback
     */
    public static void pushCallback(HandlerCallbackParam callback) {
        getInstance().callbackQueue.add(callback);
        logger.debug(">>>>>>>>>>>>>>>>>>>>>>>>> xxl-job, push callback request, logId: {}", callback.getLogId());
    }

    /**
     * 负责回调的线程, 使用HTTP发送回调消息给调度中心
     */
    private Thread triggerCallbackThread;

    /**
     * 负责处理 回调失败的信息，负责重试
     */
    private Thread triggerRetryCallbackThread;

    /**
     * 该组件是否停止
     */
    private volatile boolean toStop = false;


    /**
     * 给 triggerCallbackThread 分配任务
     */
    private void initTriggerCallbackThread() {
        triggerCallbackThread = new Thread(() -> {
            while (!toStop) {
                try {
                    // 拿出来一个任务，如果不为空说明有回调任务，将后面的所有任务全部拿出来批量执行
                    // 为什么不判空后直接拿出所有任务?
                    // 因为单纯的判空不会阻塞，没有任务就会一直循环。不如让该线程歇歇
                    HandlerCallbackParam callbackParam = getInstance().callbackQueue.take();
                    if (callbackParam == null) {
                        continue;
                    }
                    ArrayList<HandlerCallbackParam> callbackParamList = new ArrayList<>();
                    int drainToNum = getInstance().callbackQueue.drainTo(callbackParamList);
                    callbackParamList.add(callbackParam);
                    if (!CollectionUtil.isEmpty(callbackParamList)) {
                        doCallback(callbackParamList);
                    }
                } catch (InterruptedException e) {
                    if (!toStop) {
                        logger.error(e.getMessage());
                    }
                }
            }
            // 退出while循环说明xxl-job要停止了，最后将所有任务调度完
            try {
                ArrayList<HandlerCallbackParam> callbackParamList = new ArrayList<>();
                getInstance().callbackQueue.drainTo(callbackParamList);
                if (!CollectionUtil.isEmpty(callbackParamList)) {
                    doCallback(callbackParamList);
                }
            } catch (Exception e) {
                if (!toStop) {
                    logger.error(e.getMessage(), e);
                }
            }
            logger.info(">>>>>>>>>>>>> xxl-job, executor callback thread destroy");
        });
    }

    /**
     * 启动 triggerCallbackThread
     */
    private void startTriggerCallbackThread() {
        triggerCallbackThread.setDaemon(true);
        triggerCallbackThread.setName("xxl-job, executor TriggerCallbackThread");
        triggerCallbackThread.start();
    }


    /**
     * 初始化重试线程
     */
    private void initTriggerRetryCallbackThread() {
        triggerRetryCallbackThread = new Thread(() -> {
            while (!toStop) {
                try {
                    retryFailCallback();
                } catch (Exception e) {
                    if (!toStop) {
                        logger.error(e.getMessage());
                    }
                }
                // 30s一次
                try {
                    TimeUnit.SECONDS.sleep(30);
                } catch (InterruptedException e) {
                    if (!toStop) {
                        logger.error(e.getMessage());
                    }
                }
            }
            logger.info(">>>>>>>>>>> xxl-job, executor retry callback thread destroy");
        });
    }

    private void startTriggerRetryCallbackThread() {
        triggerRetryCallbackThread.setDaemon(true);
        triggerRetryCallbackThread.setName("xxl-job, executor TriggerRetryCallbackThread");
        triggerRetryCallbackThread.start();
    }


    /**
     * 启动该组件，即给组建的两个线程分配任务并启动。          <br></br>
     * triggerCallbackThread、triggerRetryCallbackThread
     */
    public void start() {
        // 如果连通信都没法通信那还回调啥
        if (XxlJobExecutor.getAdminBizList().isEmpty()) {
            logger.warn(">>>>>>>>>>> xxl-job, executor callback config fail, adminAddresses is null.");
            return;
        }
        // 给 triggerCallbackThread 分配任务 并启动它
        initTriggerCallbackThread();
        startTriggerCallbackThread();

        // 重试线程
        initTriggerRetryCallbackThread();
        startTriggerRetryCallbackThread();
    }


    /**
     * 回调, 要想每一个调度中心都发消息，但是只要有一个成功就结束。
     *
     * @param callbackParamList
     */
    private void doCallback(List<HandlerCallbackParam> callbackParamList) {
        boolean callbackRet = false;
        // 遍历给所有调度中心回调执行结果，但只要有一个成功回调就结束
        for (AdminBiz adminBiz : XxlJobExecutor.getAdminBizList()) {
            try {
                Result callbackResult = adminBiz.callback(callbackParamList);
                // 回调成功，记录日志并结束
                if (callbackResult != null && Result.SUCCESS_CODE == callbackResult.getCode()) {
                    callbackLog(callbackParamList, "<br>---------------------- xxl-job job callback finish");
                    callbackRet = true;
                    break;
                } else {
                    // 回调失败，记录日志，去其他调度中心尝试
                    callbackLog(callbackParamList, "<br>---------------------- xxl-job job callback fail, callbackResult:" + callbackResult);
                }
            } catch (Exception e) {
                // 如果出现异常，记录日志，与失败日志有所不同。
                callbackLog(callbackParamList, "<br>---------------------- xxl-job job callback error, error message:" + e.getMessage());
            }
        }
        // 如果所有调度中心都没有回调成功，将回调信息记录到日志中(一个专门的文件夹中)，方便重试线程回调
        if (!callbackRet) {
            appendFailCallbackFile(callbackParamList);
        }
    }

    /**
     * 将回调结果记录到日志中，成功/失败
     * 这个日志不用于重试
     *
     * @param callbackParamList
     * @param logContent
     */
    private void callbackLog(List<HandlerCallbackParam> callbackParamList, String logContent) {
        // 将不同的任务的日志存放在不同的文件中
        for (HandlerCallbackParam callbackParam : callbackParamList) {
            String logFileName = XxlJobFileAppender.makeLogFileName(new Date(callbackParam.getLogDateTim()), callbackParam.getLogId());
            // 只需要将日志文件名存储在 XxlJobContext 中，XxlJobHelper就可以找到。
            XxlJobContext.setXxlJobContext(new XxlJobContext(
                    -1,
                    null,
                    logFileName,
                    -1,
                    -1
            ));
            XxlJobHelper.log(logContent);
        }
    }


    /**
     * 重试日志的存储路径
     */
    private static String failCallbackFilePath = XxlJobFileAppender.getLogBasePath().concat(File.separator).concat("callbacklog").concat(File.separator);

    /**
     * 回调失败重试日志的文件名(完整名: 路径名 + 文件名), 文件名为当前时间
     */
    private static String failCallbackFileName = failCallbackFilePath.concat("xxl-job-callback-{x}").concat(".log");


    /**
     * 将回调失败的回调结果记录在日志中等待重试
     *
     * @param callbackParamList
     */
    private void appendFailCallbackFile(List<HandlerCallbackParam> callbackParamList) {
        if (CollectionUtil.isEmpty(callbackParamList)) {
            return;
        }
        // 将日志序列化后存储
        byte[] callbackParamListBytes = JdkSerializeTool.serialize(callbackParamList);
        File callbackLogFile = new File(failCallbackFileName.replace("{x}", String.valueOf(System.currentTimeMillis())));
        // 如果这个文件名已经存在，那就在后面拼接数字，如: 162948274-1、162948274-2直到拼接100
        if (callbackLogFile.exists()) {
            for (int i = 0; i < 100; i++) {
                callbackLogFile = new File(failCallbackFileName.replace("{x}", String.valueOf(System.currentTimeMillis()).concat("-").concat(String.valueOf(i))));
                if (!callbackLogFile.exists()) {
                    break;
                }
            }
        }
        // 开始写入
        FileUtil.writeFileContent(callbackLogFile, callbackParamListBytes);
    }

    /**
     * 从失败日志文件中取出回调失败的任务，重试
     */
    private void retryFailCallback() {
        File file = new File(failCallbackFilePath);
        // 如果该路径不存在或者是文件，删掉。
        // 如果该路径不是文件夹、没有子文件，就return
        if (!file.exists()) {
            return;
        }
        if (file.isFile()) {
            file.delete();
        }
        if (!(file.isDirectory() && file.list() != null && file.list().length > 0)) {
            return;
        }
        // 读取文件夹下属文件的内容，没有就删除，有就转换后调用
        for (File callbackLogFile : file.listFiles()) {
            byte[] callbackParamListBytes = FileUtil.readFileContent(callbackLogFile);
            if (callbackParamListBytes == null || callbackParamListBytes.length < 1) {
                callbackLogFile.delete();
                continue;
            }
            List<HandlerCallbackParam> callbackParamList = (List<HandlerCallbackParam>) JdkSerializeTool.deserialize(callbackParamListBytes, List.class);
            callbackLogFile.delete();
            doCallback(callbackParamList);
        }
    }


    public void toStop() {
        toStop = true;
        if (triggerCallbackThread != null) {
            triggerCallbackThread.interrupt();
            try {
                triggerCallbackThread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
        }

        if (triggerRetryCallbackThread != null) {
            triggerRetryCallbackThread.interrupt();
            try {
                triggerRetryCallbackThread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
        }
    }
}
