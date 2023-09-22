package com.xiaohe.admin.mapper;

import com.xiaohe.admin.core.model.XxlJobUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-09-22 14:59
 */
@Mapper
public interface XxlJobUserMapper {

    public List<XxlJobUser> pageList(@Param("start") int start,
                                     @Param("size") int size,
                                     @Param("username") String username,
                                     @Param("role") int role);

    public XxlJobUser loadByUsername(@Param("username") String username);

    public int add(XxlJobUser xxlJobUser);

    public int update(XxlJobUser xxlJobUser);

    public int delete(@Param("id") int id);
}
