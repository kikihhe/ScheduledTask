package com.xiaohe.ScheduledTask.admin.core.model;

/**
 * @author : 小何
 * @Description : 用户
 * @date : 2023-08-22 17:22
 */
public class ScheduledTaskUser {
    private int id;

    private String username;

    private String password;

    /**
     * 用户角色
     * 0 : 普通用户
     * 1 : 管理员
     */
    private int role;

    /**
     * 用户权限
     */
    private String permission;

}
