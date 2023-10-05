package com.xiaohe.admin.core.alarm;

import com.xiaohe.admin.core.model.XxlJobInfo;
import com.xiaohe.admin.core.model.XxlJobLog;

/**
 * @author : 小何
 * @Description : 发送邮件的顶级接口
 * @date : 2023-10-05 20:31
 */
public interface JobAlarm {
    public boolean doAlarm(XxlJobInfo xxlJobInfo, XxlJobLog xxlJobLog);
}
