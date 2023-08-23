package com.xiaohe.ScheduledTask.admin.core.scheduler;

import com.mysql.cj.log.LogFactory;
import com.xiaohe.ScheduledTask.admin.core.thread.JobScheduleHelper;
import com.xiaohe.ScheduledTask.admin.core.thread.JobTriggerPoolHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author : 小何
 * @Description : 核心调度类，初始化各种调度中心的组件，如JobScheduleHelper
 * @date : 2023-08-23 14:01
 */
public class ScheduledTaskScheduler {
    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskScheduler.class);

    public void init() {
        // 初始化线程池
        JobTriggerPoolHelper.toStart();
        // 启动JobScheduleHelper，扫描数据库的定时任务
        JobScheduleHelper.getInstance().start();

    }

}
