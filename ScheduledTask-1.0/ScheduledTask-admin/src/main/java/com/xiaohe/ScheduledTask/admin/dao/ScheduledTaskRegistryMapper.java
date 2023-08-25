package com.xiaohe.ScheduledTask.admin.dao;

import com.xiaohe.ScheduledTask.admin.core.model.ScheduledTaskRegistry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-08-25 14:12
 */
@Mapper
public interface ScheduledTaskRegistryMapper {
    /**
     * 查找过期执行器 (一定时间未更新的执行器)
     * @param timeout 指定时间
     * @param nowTime 现在的时间
     * @return
     */
    public List<Integer> findDead(@Param("timeout") int timeout,
                                  @Param("notTime") Date nowTime);

    /**
     * 删除多个执行器
     * @param ids
     * @return
     */
    public int removeDead(@Param("ids") List<Integer> ids);

    /**
     * 查出所有没下线的执行器
     * @param timeout
     * @param nowTime
     * @return
     */
    public List<ScheduledTaskRegistry> findAll(@Param("timeout") int timeout,
                                               @Param("notTime") Date nowTime);

    public int registryDelete(@Param("registryGroup") String registryGroup,
                        @Param("registryKey") String registryKey,
                        @Param("registryValue") String registryValue);

    int registryUpdate(@Param("registryGroup") String registryGroup,
                       @Param("registryKey") String registryKey,
                       @Param("registryValue") String registryValue,
                       @Param("updateTime") Date nowTime);

    public int registrySave(@Param("registryGroup") String registryGroup,
                            @Param("registryKey") String registryKey,
                            @Param("registryValue") String registryValue,
                            @Param("updateTime") Date updateTime);
}
