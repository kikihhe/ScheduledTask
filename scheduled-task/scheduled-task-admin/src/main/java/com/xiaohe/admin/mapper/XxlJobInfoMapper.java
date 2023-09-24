package com.xiaohe.admin.mapper;

import com.xiaohe.admin.core.model.XxlJobInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-09-23 14:23
 */
@Mapper
public interface XxlJobInfoMapper {
    public XxlJobInfo loadById(@Param("jobId") Integer jobId);

    public List<XxlJobInfo> getJobsByJobGroup(@Param("jobGroup") Integer jobGroup);
}
