package com.xiaohe.admin.mapper;

import com.xiaohe.admin.core.model.XxlJobRegistry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-10-06 14:49
 */
@Mapper
public interface XxlJobRegistryMapper {

    /**
     * 查找需要删除的执行器(注册记录)
     * @param timeout 达到删除的条件
     * @param nowTime 当前时间
     * @return
     */
    public List<Integer> findDead(@Param("timeout") int timeout,
                                  @Param("nowTime") Date nowTime);

    /**
     * 查出所有没过期的执行器注册信息
     * @param timeout
     * @param nowTime
     * @return
     */
    public List<XxlJobRegistry> findAll(@Param("timeout") int timeout,
                                        @Param("nowTime") Date nowTime);

    /**
     * 删除过期的注册信息
     * @param ids
     * @return
     */
    public int removeDead(List<Integer> ids);

    /**
     * 将指定的注册信息删除
     * @param registryGroup
     * @param registryKey
     * @param registryValue
     * @return
     */
    public int registryDelete(@Param("registryGroup") String registryGroup,
                              @Param("registryKey") String registryKey,
                              @Param("registryValue") String registryValue);

    public int registryUpdate(@Param("registryGroup") String registryGroup,
                              @Param("registryKey") String registryKey,
                              @Param("registryValue") String registryValue,
                              @Param("updateTime") Date updateTime);

    public int registrySave(@Param("registryGroup") String registryGroup,
                            @Param("registryKey") String registryKey,
                            @Param("registryValue") String registryValue,
                            @Param("updateTime") Date updateTime);
}
