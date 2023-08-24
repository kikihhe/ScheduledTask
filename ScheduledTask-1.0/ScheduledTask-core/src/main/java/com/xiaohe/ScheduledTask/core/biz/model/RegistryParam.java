package com.xiaohe.ScheduledTask.core.biz.model;

import java.io.Serializable;

/**
 * @author : 小何
 * @Description : 将执行器注册到调度中心需要发送的信息
 * @date : 2023-08-23 12:46
 */
public class RegistryParam implements Serializable {
    private static final long serialVersionUID = 42L;

    /**
     * 注册方式
     */
    private String registryGroup;

    /**
     * 执行器标识, appname
     */
    private String registryKey;

    /**
     * 执行器的地址，ip
     */
    private String registryValue;


    public RegistryParam() {
    }
    public RegistryParam(String registryGroup, String registryKey, String registryValue) {
        this.registryGroup = registryGroup;
        this.registryKey = registryKey;
        this.registryValue = registryValue;
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
}
