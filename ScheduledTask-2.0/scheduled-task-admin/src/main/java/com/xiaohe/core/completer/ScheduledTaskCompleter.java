package com.xiaohe.core.completer;

import com.xiaohe.core.conf.ScheduledTaskAdminConfig;
import com.xiaohe.core.model.ScheduledTaskLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author : 小何
 * @Description : 更新日志信息，触发子任务的类
 * @date : 2023-09-07 11:27
 */
public class ScheduledTaskCompleter {
    private static Logger logger = LoggerFactory.getLogger(ScheduledTaskCompleter.class);

    /**
     * 触发子任务、更新数据库信息
     * @param log
     * @return
     */
    public static int updateHandleInfoAndFinish(ScheduledTaskLog log) {
        // 触发子任务
//        finishJob(log);
        // 将本条日志更新到数据库
        return ScheduledTaskAdminConfig.getAdminConfig().getScheduledTaskLogMapper().updateHandleInfo(log);
    }

}
