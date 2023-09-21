package com.xiaohe.admin.core.model;

import java.util.Date;

/**
 * @author : 小何
 * @Description : 执行器的注册信息，每一个执行器对应一个XxlJobRegistry
 * @date : 2023-09-21 18:20
 */
public class XxlJobRegistry {
    private int id;
    /**
     * 注册方式 0自动，1手动
     */
    private String registryGroup;

    /**
     * 执行器的appname, 即该执行器属于哪个执行器组
     */
    private String registryKey;

    /**
     * 该执行器IP
     */
    private String registryValue;

    /**
     * 最近一次注册时间
     */
    private Date updateTime;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRegistryGroup() {
        return registryGroup;
    }

    public void setRegistryGroup(String registryGroup) {
        this.registryGroup = registryGroup;
    }

    public String getRegistryKey() {
        return registryKey;
    }

    public void setRegistryKey(String registryKey) {
        this.registryKey = registryKey;
    }

    public String getRegistryValue() {
        return registryValue;
    }

    public void setRegistryValue(String registryValue) {
        this.registryValue = registryValue;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
