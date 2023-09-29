package com.xiaohe.core.context;

import com.xiaohe.core.log.XxlJobFileAppender;
import com.xiaohe.core.util.DateUtil;
import com.xiaohe.core.util.StringUtil;
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
 * @date : 2023-09-29 16:25
 */
public class XxlJobHelper {
    private static Logger logger = LoggerFactory.getLogger("xxl-job logger");

    /**
     * 拼接详细信息(class method lineNumber thread)，调用XxlJobFileAppender记录日志
     * @param callInfo
     * @param appendLog
     * @return
     */
    private static boolean logDetail(StackTraceElement callInfo, String appendLog) {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return false;
        }
        // 拼接一下完整的信息
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(DateUtil.formatDate(new Date())).append(" ")
                .append("[" + callInfo.getClassName() + "#" + callInfo.getMethodName() + "]").append("-")
                .append("[" + callInfo.getLineNumber() + "]").append("-")
                .append("[" + Thread.currentThread().getName() + "]").append("-")
                .append(appendLog != null ? appendLog : "");
        String formatAppendLog = stringBuffer.toString();
        String logFileName = xxlJobContext.getJobLogFileName();
        if (!StringUtil.hasText(logFileName)) {
            logger.info(">>>>>>>>>> {}", formatAppendLog);
            return false;
        }
        // 调用 XxlJobFileAppender 写日志
        XxlJobFileAppender.appendLog(logFileName, formatAppendLog);
        return true;
    }


    /**
     * 写入日志，写入哪个文件？在XxlJobContext中获取
     * @param appendLogPattern 日志表达式，如 "xxl job thread - {}"
     * @param appendLogArgument 取代{}的对象，如 1
     * @return
     */
    public static boolean log(String appendLogPattern, Object...appendLogArgument) {
        FormattingTuple ft = MessageFormatter.arrayFormat(appendLogPattern, appendLogArgument);
        String appendLog = ft.getMessage();
        // StackTraceElement 用于获取当前 class method thread等信息
        StackTraceElement callInfo = new Throwable().getStackTrace()[1];
        return logDetail(callInfo, appendLog);
    }


    /**
     * 将异常写入文件
     * @param e
     * @return
     */
    public static boolean log(Throwable e) {
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        String appendLog = stringWriter.toString();
        StackTraceElement callInfo = new Throwable().getStackTrace()[1];
        return logDetail(callInfo, appendLog);
    }







    /**
     * 获取定时任务的id
     * @return
     */
    public static long getJobId() {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return -1;
        }
        return xxlJobContext.getJobId();
    }

    /**
     * 获取执行参数
     * @return
     */
    public static String getJobParam() {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return null;
        }
        return xxlJobContext.getJobParam();
    }

    /**
     * 获取记录该任务的日志名
     * @return
     */
    public static String getJobLogFileName() {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return null;
        }
        return xxlJobContext.getJobLogFileName();
    }

    public static int getShardIndex() {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return -1;
        }
        return xxlJobContext.getShardIndex();
    }

    public static int getShardTotal() {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return -1;
        }
        return xxlJobContext.getShardTotal();
    }

    // -----------------------------------------------------------------------------------------
    // 把定时任务执行结果保存在 XxlJobContext 中
    public static boolean handleResult(int handleCode, String handleMsg) {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return false;
        }
        xxlJobContext.setHandleCode(handleCode);
        xxlJobContext.setHandleMsg(handleMsg);
        return true;
    }

    public static boolean handleSuccess(){
        return handleResult(XxlJobContext.HANDLE_CODE_SUCCESS, null);
    }


    public static boolean handleSuccess(String handleMsg) {
        return handleResult(XxlJobContext.HANDLE_CODE_SUCCESS, handleMsg);
    }


    public static boolean handleFail(){
        return handleResult(XxlJobContext.HANDLE_CODE_FAIL, null);
    }


    public static boolean handleFail(String handleMsg) {
        return handleResult(XxlJobContext.HANDLE_CODE_FAIL, handleMsg);
    }


    public static boolean handleTimeout(){
        return handleResult(XxlJobContext.HANDLE_CODE_TIMEOUT, null);
    }


    public static boolean handleTimeout(String handleMsg){
        return handleResult(XxlJobContext.HANDLE_CODE_TIMEOUT, handleMsg);
    }




}
