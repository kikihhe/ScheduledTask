package com.xiaohe.admin.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-09-27 16:41
 */
@Mapper
public interface XxlJobLogGlueMapper {
    void deleteByJobId(@Param("id") Integer id);
}
