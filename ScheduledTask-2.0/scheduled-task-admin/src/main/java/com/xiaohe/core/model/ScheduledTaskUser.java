package com.xiaohe.core.model;


import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author : 小何
 * @Description : 用户信息类
 * @date : 2023-08-31 13:20
 */
public class ScheduledTaskUser {

    private int id;

    private String username;

    private String password;

    /**
     * 用户的角色
     * 0 : 普通用户
     * 1 : 管理员
     */
    private int role;

    /**
     * 用户权限, 包含该用户可以操作的所有执行器组id，以逗号隔开
     * 例: 1,4,23 代表该用户可以操作 id为 1、4、23的执行器组。
     * 如果执行器在这些组中，那么该用户就可以操作这个执行器
     */
    private String permission;


    /**
     * 判断该用户是否有权限操作指定执行器组内的执行器
     * @param jobGroup
     * @return
     */
    public boolean validPermission(int jobGroup) {
        if (this.role == 1) {
            return true;
        }
        if (StringUtils.hasText(this.permission)) {
            ArrayList<String> permissionList = new ArrayList<>(Arrays.asList(this.permission.split(",")));
            for (String permissionGroup : permissionList) {
                if (permissionGroup.equals(String.valueOf(jobGroup))) {
                    return true;
                }
            }
        }
        return false;

    }

    @Override
    public String toString() {
        return "ScheduledTaskUser{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", role=" + role +
                ", permission='" + permission + '\'' +
                '}';
    }

}
