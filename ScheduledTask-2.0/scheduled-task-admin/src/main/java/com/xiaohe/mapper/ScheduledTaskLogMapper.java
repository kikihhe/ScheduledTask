package com.xiaohe.mapper;

import com.xiaohe.core.model.ScheduledTaskLog;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-08-31 16:53
 */
public interface ScheduledTaskLogMapper {
    public long save(ScheduledTaskLog jobLog);

    public int updateTriggerInfo(ScheduledTaskLog log);
}
