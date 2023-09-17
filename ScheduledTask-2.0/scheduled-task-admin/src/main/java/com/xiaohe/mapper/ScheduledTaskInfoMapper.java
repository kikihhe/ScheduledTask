package com.xiaohe.mapper;

import com.xiaohe.core.model.ScheduledTaskInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-08-31 16:52
 */
@Mapper
public interface ScheduledTaskInfoMapper {

    public ScheduledTaskInfo loadById(@Param("jobId") int jobId);

    public List<ScheduledTaskInfo> scheduleJobQuery(@Param("maxNextTime") long maxNextTime, @Param("pagesize") int pagesize);

    public int scheduleUpdate(@Param("jobList") List<ScheduledTaskInfo> scheduledTaskInfo);

}
