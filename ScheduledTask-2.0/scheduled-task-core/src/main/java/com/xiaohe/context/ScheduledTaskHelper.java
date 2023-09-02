package com.xiaohe.context;

import com.xiaohe.log.ScheduledTaskFileAppender;
import com.xiaohe.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

/**
 * @author : 小何
 * @Description : 对日志进行处理
 * @date : 2023-09-02 16:17
 */
public class ScheduledTaskHelper {

    private static Logger logger = LoggerFactory.getLogger("scheduled-task logger");
    /**
     * 从上下文中获取定时任务id
     * @return
     */
    public static long getJobId() {
        ScheduledTaskContext scheduledTaskContext = ScheduledTaskContext.getScheduledTaskContext();
        if (scheduledTaskContext == null) {
            return -1;
        }
        return scheduledTaskContext.getJobId();
    }

    /**
     * 获取定时任务的参数
     * @return
     */
    public static String getJobParam() {
        ScheduledTaskContext scheduledTaskContext = ScheduledTaskContext.getScheduledTaskContext();
        if (scheduledTaskContext == null) {
            return null;
        }
        return scheduledTaskContext.getJobParam();
    }

    /**
     * 获取记录该任务执行日志的文件名
     * @return
     */
    public static String getJobLogFileName() {
        ScheduledTaskContext scheduledTaskContext = ScheduledTaskContext.getScheduledTaskContext();
        if (scheduledTaskContext == null) {
            return null;
        }
        return scheduledTaskContext.getJobLogFileName();
    }

    /**
     * 获取分片索引
     * @return
     */
    public static int getShardIndex() {
        ScheduledTaskContext scheduledTaskContext = ScheduledTaskContext.getScheduledTaskContext();
        if (scheduledTaskContext == null) {
            return -1;
        }
        return scheduledTaskContext.getShardIndex();
    }

    /**
     * 获取分片总数
     * @return
     */
    public static int getShardTotal() {
        ScheduledTaskContext scheduledTaskContext = ScheduledTaskContext.getScheduledTaskContext();
        if (scheduledTaskContext == null) {
            return -1;
        }
        return scheduledTaskContext.getShardTotal();
    }


    /**
     * 记录日志
     * @param appendLogPattern 需要记录的字符串(有的数据是{})，如: "id={}, name={}"
     * @param appendLogArguments 替换 上面字符串中{}的数据
     * @return
     */
    public static boolean log(String appendLogPattern, Object... appendLogArguments) {
        // MessageFormatter.arrayFormat : 按照slf4j的方式对字符串进行格式化
        // 例: log.error("id={}, name={}", 1, "小明");
        // MessageFormatter 可以将字符串中的 {} 换成后面的参数
        FormattingTuple ft = MessageFormatter.arrayFormat(appendLogPattern, appendLogArguments);
        String appendLog = ft.getMessage();
        StackTraceElement callInfo = new Throwable().getStackTrace()[1];
        return logDetail(callInfo, appendLog);
    }

    /**
     * 将异常写入文件中
     * @param e
     * @return
     */
    public static boolean log(Throwable e) {
        StringWriter sw = new StringWriter();
        // 将异常中的信息写入sw中
        e.printStackTrace(new PrintWriter(sw));
        String appendLog = sw.toString();
        StackTraceElement callInfo = new Throwable().getStackTrace()[1];
        return logDetail(callInfo, appendLog);
    }


    /**
     * 操作 ScheduledTaskFileAppender 写文件的类
     * @param callInfo
     * @param appendLog
     * @return
     */
    private static boolean logDetail(StackTraceElement callInfo, String appendLog) {
        ScheduledTaskContext context = ScheduledTaskContext.getScheduledTaskContext();
        if (context == null) {
            return false;
        }
        // 拼接详细信息
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(DateUtil.formatDateTime(new Date())).append(" ")
                .append("[" + callInfo.getClassName() + "#" + callInfo.getMethodName() + "]").append("-")
                .append("[" + callInfo.getLineNumber() + "]").append("-")
                .append("[" + Thread.currentThread().getName() + "]").append(" ")
                .append(appendLog != null ? appendLog : "");
        String formatAppendLog = stringBuffer.toString();
        String logFileName = context.getJobLogFileName();
        if (logFileName != null && !logFileName.trim().isEmpty()) {
            // 操作文件去写
            ScheduledTaskFileAppender.appendLog(logFileName, formatAppendLog);
            return true;
        } else {
            logger.info(">>>>>>>>>>>>{}", formatAppendLog);
            return false;
        }
    }

    /**
     * 将执行结果存入 ScheduledTaskContext 中
     * @param handleCode
     * @param handleMse
     * @return
     */
    public static boolean handleResult(int handleCode, String handleMse) {
        ScheduledTaskContext context = ScheduledTaskContext.getScheduledTaskContext();
        if (context == null) {
            return false;
        }
        context.setHandleMsg(handleMse);
        context.setHandleCode(handleCode);
        return true;
    }

    public static boolean handleTimeout(String handleMsg) {
        return handleResult(ScheduledTaskContext.HANDLE_CODE_TIMEOUT, handleMsg);
    }

    public static boolean handleTimeout(){
        return handleResult(ScheduledTaskContext.HANDLE_CODE_TIMEOUT, null);
    }

    public static boolean handleFail(String handleMsg) {
        return handleResult(ScheduledTaskContext.HANDLE_CODE_FAIL, handleMsg);
    }

    public static boolean handleFail(){
        return handleResult(ScheduledTaskContext.HANDLE_CODE_FAIL, null);
    }

    public static boolean handleSuccess(){
        return handleResult(ScheduledTaskContext.HANDLE_CODE_SUCCESS, null);
    }


    public static boolean handleSuccess(String handleMsg) {
        return handleResult(ScheduledTaskContext.HANDLE_CODE_SUCCESS, handleMsg);
    }








}
