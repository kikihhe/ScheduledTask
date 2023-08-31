package com.xiaohe.core.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author : 小何
 * @Description : 执行器组
 * @date : 2023-08-31 13:11
 */
public class ScheduledTaskGroup {
    private int id;

    /**
     * 项目名称
     */
    private String appname;

    /**
     * 中文名称
     */
    private String title;

    /**
     * 注册类型
     * 0 : 自动注册
     * 1 : 手动注册
     */
    private int addressType;

    /**
     * 该组中所有执行器的地址, 以逗号隔开
     */
    private String addressList;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 将 addressList 以逗号分割的IP地址转为字符串集合
     */
    private List<String> registryList;

    /**
     * 获取group中所有执行器IP地址
     * @return
     */
    public List<String> getRegistryList() {
        if (addressList != null && !addressList.trim().isEmpty()) {
            registryList = new ArrayList<>(Arrays.asList(addressList.split(",")));
        }
        return registryList;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAppname() {
        return appname;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getAddressType() {
        return addressType;
    }

    public void setAddressType(int addressType) {
        this.addressType = addressType;
    }

    public String getAddressList() {
        return addressList;
    }

    public void setAddressList(String addressList) {
        this.addressList = addressList;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
