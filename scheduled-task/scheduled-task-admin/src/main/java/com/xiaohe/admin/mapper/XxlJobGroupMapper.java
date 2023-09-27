package com.xiaohe.admin.mapper;

import com.xiaohe.admin.core.model.XxlJobGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-09-22 15:02
 */
@Mapper
public interface XxlJobGroupMapper {

    /**
     * 查找所有执行器组
     * @return
     */
    public List<XxlJobGroup> findAll();

    public XxlJobGroup load(@Param("jobGroup") int jobGroup);
}
