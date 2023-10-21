package com.xiaohe.admin.mapper;

import com.xiaohe.admin.core.model.XxlJobLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-09-23 14:22
 */
@Mapper
public interface XxlJobLogMapper {
    /**
     * 保存，返回主键
     * @param xxlJobLog
     * @return
     */
    public long save(XxlJobLog xxlJobLog);

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

    public XxlJobLog loadById(@Param("logId") Long logId);

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


    void delete(@Param("id") Integer id);

    public List<XxlJobLog> findFailJobLogs(@Param("count") int count);

    /**
     * 修改任务的告警状态
     * @param logId
     * @param oldAlarmStatus
     * @param newAlarmStatus
     * @return
     */
    public int updateAlarmStatusInt(@Param("logId") long logId,
                                    @Param("oldAlarmStatus") int oldAlarmStatus,
                                    @Param("newAlarmStatus") int newAlarmStatus);

    /**
     * 调度完（执行前）更新调度信息
     * @param xxlJobLog
     * @return
     */
    public int updateTriggerInfo(XxlJobLog xxlJobLog);

    /**
     * 执行完更新执行信息
     * @param xxlJobLog
     * @return
     */
    int updateHandleInfo(XxlJobLog xxlJobLog);

    /**
     * 找到10分钟内 调度了 && 没执行 && 执行器宕机 的log
     * @param losedTime
     */
    public List<Long> findLostJobIds(@Param("losedTime") Date losedTime);

    public Map<String, Object> findLogReport(@Param("from")Date from,
                                             @Param("to") Date to);

    public int pageListCount(@Param("offset") int offset,
                             @Param("pagesize") int pagesize,
                             @Param("jobGroup") int jobGroup,
                             @Param("jobId") int jobId,
                             @Param("triggerTimeStart") Date triggerTimeStart,
                             @Param("triggerTimeEnd") Date triggerTimeEnd,
                             @Param("logStatus") int logStatus);
    public XxlJobLog load(@Param("id") long id);


}
