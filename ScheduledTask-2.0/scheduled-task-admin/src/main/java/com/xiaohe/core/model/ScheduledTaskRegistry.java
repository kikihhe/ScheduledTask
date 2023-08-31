package com.xiaohe.core.model;

import java.util.Date;

/**
 * @author : 小何
 * @Description : 调度中心持有的执行器发送的注册信息
 * @date : 2023-08-31 13:17
 */
public class ScheduledTaskRegistry {
    private int id;

    /**
     * 执行器的注册方式，手动还是自动
     */
    private String registryGroup;

    /**
     * appname, 即这个执行器属于哪个group，
     */
    private String registryKey;

    /**
     * 执行器的IP地址
     */
    private String registryValue;

    /**
     * 更新时间
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
