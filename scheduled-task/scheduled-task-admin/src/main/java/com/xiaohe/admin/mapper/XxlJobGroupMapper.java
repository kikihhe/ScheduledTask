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

    /**
     * 查找自动注册的执行器组
     * @param addressType
     * @return
     */
    public List<XxlJobGroup> findByAddressType(@Param("addressType") Integer addressType);

    public int updateBatch(@Param("groupList") List<XxlJobGroup> groupList);

    public List<XxlJobGroup> pageList(@Param("offset") int offset,
                                      @Param("pagesize") int pagesize,
                                      @Param("appname") String appname,
                                      @Param("title") String title);

    public int pageListCount(@Param("offset") int offset,
                             @Param("pagesize") int pagesize,
                             @Param("appname") String appname,
                             @Param("title") String title);

    public int save(XxlJobGroup xxlJobGroup);

    public int update(XxlJobGroup xxlJobGroup);
    public int remove(@Param("id") int id);
}
