package com.xiaohe.core.thread;

import com.xiaohe.biz.model.HandleCallbackParam;
import com.xiaohe.biz.model.Result;
import com.xiaohe.core.completer.ScheduledTaskCompleter;
import com.xiaohe.core.conf.ScheduledTaskAdminConfig;
import com.xiaohe.core.model.ScheduledTaskLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author : 小何
 * @Description : 调度中心接收执行器回调信息的工作组件
 * @date : 2023-09-07 11:09
 */
public class JobCompleteHelper {
    private static Logger logger = LoggerFactory.getLogger(JobCompleteHelper.class);
    private static JobCompleteHelper instance = new JobCompleteHelper();

    public static JobCompleteHelper getInstance(){
        return instance;
    }


    /**
     * 处理执行器回调回来的日志信息
     */
    private ThreadPoolExecutor callbackThreadPool = null;

    /**
     * 监控线程
     */
    private Thread monitorThread;

    /**
     * 该组件是否结束
     */
    private volatile boolean toStop = false;


    public Result<String> callback(List<HandleCallbackParam> callbackParamList) {
        callbackThreadPool.execute(() -> {
            for (HandleCallbackParam handleCallbackParam : callbackParamList) {
                Result<String> callbackResult = callback(handleCallbackParam);
            }
        });
        return Result.SUCCESS;
    }

    private Result<String> callback(HandleCallbackParam handleCallbackParam) {
        ScheduledTaskLog log = ScheduledTaskAdminConfig.getAdminConfig().getScheduledTaskLogMapper().load(handleCallbackParam.getLogId());
        // 如果为空，说明没有对应的日志，如果响应码大于0，说明已经处理过了。
        if (log == null) {
            return new Result<String>(Result.FAIL_CODE, "job log not found");
        }
        if (log.getHandleCode() > 0) {
            return new Result<String>(Result.FAIL_CODE, "log repeate callback");
        }
        StringBuffer handleMsg = new StringBuffer();
        if (log.getHandleMsg() != null) {
            handleMsg.append(log.getHandleMsg())
                    .append("<br>");
        }
        log.setHandleTime(new Date());
        log.setHandleCode(handleCallbackParam.getHandleCode());
        log.setHandleMsg(handleMsg.toString());
        // 触发子任务的处理，处理完成后将日志刷新到数据库
        ScheduledTaskCompleter.updateHandleInfoAndFinish(log);
        return Result.SUCCESS;
    }
}
