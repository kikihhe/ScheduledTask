package com.xiaohe.ScheduledTask.admin.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-08-22 21:22
 */@Mapper
public interface ScheduledTaskInfoMapper {
    /**
     * 查询所有定时任务数量
     */
    public int findAllCount();

    /**
     * 根据id修改定时任务的定时信息，如上次调度时间、下次调度时间、调度状态
     * @param scheduledTaskInfo
     * @return 影响行数
     */
    public int scheduleUpdate(ScheduledTaskInfoMapper scheduledTaskInfo);

    /**
     * 修改定时任务的全部信息
     * @param scheduledTaskInfo
     * @return
     */
    public int update(ScheduledTaskInfoMapper scheduledTaskInfo);

    /**
     * 根据id删除定时任务
     * @param id 需要删除的定时任务的id
     * @return 影响行数
     */
    public int delete(@Param("id") int id);

    /**
     * 根据id查询定时任务信息
     * @param id
     * @return
     */
    public ScheduledTaskInfoMapper loadById(@Param("id") int id);

    /**
     * 新增定时任务
     * @param scheduledTaskInfo
     * @return
     */
    public int save(ScheduledTaskInfoMapper scheduledTaskInfo);




}
