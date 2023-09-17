package com.xiaohe.core.alarm;

import com.xiaohe.core.model.ScheduledTaskInfo;
import com.xiaohe.core.model.ScheduledTaskLog;

/**
 * @author : 小何
 * @Description : 邮件告警接口
 * @date : 2023-09-17 12:49
 */
public interface JobAlarm {
    public boolean doAlarm(ScheduledTaskInfo jobInfo, ScheduledTaskLog jobLog);
}
