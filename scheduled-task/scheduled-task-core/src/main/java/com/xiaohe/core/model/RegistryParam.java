package com.xiaohe.core.model;

import java.io.Serializable;

/**
 * @author : 小何
 * @Description : 执行器给调度中心发送的心跳
 * @date : 2023-09-23 12:12
 */
public class RegistryParam implements Serializable {
    private static final long serialVersionUID = 42L;

    /**
     * 注册方式
     */
    private String registryGroup;

    /**
     * 执行器名称, 即appname, 即执行器组的名字
     */
    private String registryKey;

    /**
     * 该执行器的IP
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
