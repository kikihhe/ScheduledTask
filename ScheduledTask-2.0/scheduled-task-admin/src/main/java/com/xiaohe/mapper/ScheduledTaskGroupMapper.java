package com.xiaohe.mapper;

import com.xiaohe.core.model.ScheduledTaskGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-08-31 16:52
 */
@Mapper
public interface ScheduledTaskGroupMapper {

    public ScheduledTaskGroup loadById(@Param("id") int jobGroupId);

    public List<ScheduledTaskGroup> findByAddressType(@Param("addressType") Integer addressType);

    public int update(ScheduledTaskGroup xxlJobGroup);


}
