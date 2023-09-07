package com.xiaohe.mapper;

import com.xiaohe.biz.model.RegistryParam;
import com.xiaohe.core.model.ScheduledTaskRegistry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-08-31 16:53
 */
@Mapper
public interface ScheduledTaskRegistryMapper {

    /**
     * 在 scheduled_task_registry表中查找 90s未刷新的执行器
     * @param timeout
     * @param nowTime
     * @return
     */
    public List<Integer> findDead(@Param("timeout") int timeout, @Param("nowTime") Date nowTime);

    public int removeDead(@Param("ids") List<Integer> ids);


    /**
     * 查找所有未未宕机的执行器
     * @param timeout
     * @param nowTime
     * @return
     */
    public List<ScheduledTaskRegistry> findAll(@Param("timeout") int timeout, @Param("nowTime") Date nowTime);

    public int registryUpdate(@Param("registryGroup") String registryGroup,
                              @Param("registryKey") String registryKey,
                              @Param("registryValue") String registryValue,
                              @Param("updateTime") Date updateTime);

    public int registrySave(RegistryParam registryParam, Date updateTime);

    public int registryDelete(@Param("registryGroup") String registryGroup,
                              @Param("registryKey") String registryKey,
                              @Param("registryValue") String registryValue);
}
