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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getRole() {
        return role;
    }

    public void setRole(int role) {
        this.role = role;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }
}
