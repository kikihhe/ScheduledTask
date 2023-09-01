package com.xiaohe.mapper;

import com.xiaohe.core.model.ScheduledTaskInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-08-31 16:52
 */
@Mapper
public interface ScheduledTaskInfoMapper {

    public ScheduledTaskInfo loadById(@Param("jobId") int jobId);

}
