package com.xiaohe.admin.mapper;

import com.xiaohe.admin.core.model.XxlJobLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-09-23 14:22
 */
@Mapper
public interface XxlJobLogMapper {
    /**
     * 根据执行器组和执行器id查询对应日志，可以指定调度日期
     * @param start
     * @param length
     * @param jobGroup
     * @param jobId
     * @param triggerTimeStart
     * @param triggerTimeEnd
     * @param logStatus
     * @return
     */
    public List<XxlJobLog> pageList(@Param("start") int start,
                                    @Param("size") int length,
                                    @Param("jobGroup") int jobGroup,
                                    @Param("jobId") int jobId,
                                    @Param("triggerTimeStart") Date triggerTimeStart,
                                    @Param("triggerTimeEnd") Date triggerTimeEnd,
                                    @Param("logStatus") int logStatus);

    public XxlJobLog loadById(@Param("logId") Integer logId);

    /**
     * 查找指定日期/条数的日志，每次最多1000条
     * @param jobGroup
     * @param jobId
     * @param clearBeforeTime
     * @param clearBeforeNum
     * @param size
     * @return
     */
    public List<Long> findClearJobLog(@Param("jobGroup") int jobGroup,
                               @Param("jobId") int jobId,
                               @Param("clearBeforeTime") Date clearBeforeTime,
                               @Param("clearBeforeNum") int clearBeforeNum,
                               @Param("size") int size);

    public int clearLogs(@Param("logIds") List<Long> logIds);


}
