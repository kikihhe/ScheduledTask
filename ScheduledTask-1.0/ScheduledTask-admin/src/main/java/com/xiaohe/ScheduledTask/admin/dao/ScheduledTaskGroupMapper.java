package com.xiaohe.ScheduledTask.admin.dao;

import com.xiaohe.ScheduledTask.admin.core.model.ScheduledTaskGroup;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author : 小何
 * @Description : 操作执行器的mapper
 * @date : 2023-08-22 22:54
 */
public interface ScheduledTaskGroupMapper {
    /**
     * 查询所有执行器
     * @return
     */
    public List<ScheduledTaskGroup> findAll();

    /**
     * 根据录入方式查找执行器
     * @param addressType
     * @return
     */
    public List<ScheduledTaskGroup> findByAddressType(int addressType);

    /**
     * 保存执行器
     * @param scheduledTaskGroup
     * @return
     */
    public int save(ScheduledTaskGroup scheduledTaskGroup);

    /**
     * 修改执行器
     * @param scheduledTaskGroup
     * @return
     */
    public int update(ScheduledTaskGroup scheduledTaskGroup);

    /**
     * 删除执行器
     * @param id
     * @return
     */
    public int remove(@Param("id") int id);

    /**
     * 通过id查找执行器
     * @param id
     * @return
     */
    public ScheduledTaskGroup load(@Param("id") int id);

    public List<ScheduledTaskGroup> pageList(@Param("offset") int offset,
                                             @Param("pagesize") int pagesize,
                                             @Param("appname") String appname,
                                             @Param("title") String title);
    public int pageListCount(@Param("offset") int offset,
                             @Param("pagesize") int pagesize,
                             @Param("appname") String appname,
                             @Param("title") String title);


}
