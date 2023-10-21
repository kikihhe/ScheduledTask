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

    public List<XxlJobInfo> pageList(@Param("start") int start,
                                     @Param("length") int length,
                                     @Param("jobGroup") int jobGroup,
                                     @Param("triggerStatus") int triggerStatus,
                                     @Param("jobDesc") String jobDesc,
                                     @Param("executorHandler") String executorHandler,
                                     @Param("author") String author);

    public int save(XxlJobInfo jobInfo);

    void delete(@Param("id") Integer id);

    public List<XxlJobInfo> scheduleJobQuery(@Param("maxNextTime") long maxNextTime, @Param("maxCount") int maxCount);

    int scheduleUpdate(@Param("xxlJobInfos") List<XxlJobInfo> xxlJobInfos);
}
