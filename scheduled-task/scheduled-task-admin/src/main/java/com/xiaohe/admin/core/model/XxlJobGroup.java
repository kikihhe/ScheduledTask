package com.xiaohe.admin.core.model;

import com.xiaohe.core.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author : 小何
 * @Description : 执行器组
 * @date : 2023-09-21 17:06
 */
public class XxlJobGroup {
    /**
     * 执行器组id
     */
    private int id;

    /**
     * 执行器组名称
     */
    private String appname;

    /**
     * 中文名称
     */
    private String title;

    /**
     * 注册方式
     * 0: 自动注册
     * 1: 手动注册
     */
    private int addressType;

    /**
     * 执行器的地址，多个地址之间以逗号隔开
     */
    private String addressList;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 执行器地址，addressList以逗号分割后的集合
     */
    private List<String> registryList;

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

    /**
     * 获取集合形式的执行器地址
     * @return
     */
    public List<String> getRegistryList() {
        if (StringUtil.hasText(addressList)) {
            registryList = new ArrayList<>(Arrays.asList(addressList.split(",")));
        }
        return registryList;
    }

}
