package com.xiaohe.biz.model;

import java.io.Serializable;

/**
 * @author : 小何
 * @Description : 执行器注册
 * @date : 2023-09-01 18:29
 */
public class RegistryParam implements Serializable {
    private static final long serialVersionUID = 42L;

    /**
     * 注册方式
     * 0: 自动
     * 1: 手动
     */
    private String registryGroup;

    /**
     * appname
     */
    private String registryKey;

    /**
     * 执行器的地址
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
