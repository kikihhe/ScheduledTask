package com.xiaohe.admin.core.model;

import com.xiaohe.util.StringUtil;

import java.util.Objects;

/**
 * @author : 小何
 * @Description : 用户
 * @date : 2023-09-21 21:45
 */
public class XxlJobUser {
    private int id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 用户角色
     * 0: 普通用户
     * 1: 管理员
     */
    private int role;

    /**
     * 用户对应的权限
     */
    private String permission;

    /**
     * 判断当前登录用户是否有权限操作指定的执行器组
     * @param jobGroup 执行器组id
     * @return
     */
    public boolean validPermission(int jobGroup) {
        if (this.role == 1) {
            return true;
        }
        if (StringUtil.hasText(permission)) {
            for (String permissionItem : permission.split(",")) {
                if (String.valueOf(jobGroup).equals(permissionItem)) {
                    return true;
                }
            }
        }
        return false;

    }



    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public XxlJobUser setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public XxlJobUser setPassword(String password) {
        this.password = password;
        return this;
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

    @Override
    public String toString() {
        return "XxlJobUser {" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", role=" + role +
                ", permission='" + permission + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        XxlJobUser that = (XxlJobUser) o;
        return id == that.id && role == that.role && Objects.equals(username, that.username) && Objects.equals(password, that.password) && Objects.equals(permission, that.permission);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, password, role, permission);
    }
}
