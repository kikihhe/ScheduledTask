package com.xiaohe.ScheduledTask.admin.dao;

import com.xiaohe.ScheduledTask.admin.core.model.ScheduledTaskUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-08-22 21:34
 */
@Mapper
public interface ScheduledTaskUserMapper {

    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return
     */
    public ScheduledTaskUser loadByUserName(@Param("username") String username);

    /**
     * 新增用户
     * @param scheduledTaskUser
     * @return
     */
    public int save(ScheduledTaskUser scheduledTaskUser);

    /**
     * 根据id删除用户
     * @param id
     * @return
     */
    public int delete(int id);

    /**
     * 修改用户信息
     * @param scheduledTaskUser
     * @return
     */
    public int update(ScheduledTaskUser scheduledTaskUser);

    /**
     * 根据username模糊查询+分页查询
     * @param offset
     * @param pagesize
     * @param username
     * @param role
     * @return
     */
    public List<ScheduledTaskUser> pageList(@Param("offset") int offset,
                                            @Param("pagesize") int pagesize,
                                            @Param("username") String username,
                                            @Param("role") int role);

    public int pageListCount(@Param("offset") int offset,
                             @Param("pagesize") int pagesize,
                             @Param("username") String username,
                             @Param("role") int role);

}
