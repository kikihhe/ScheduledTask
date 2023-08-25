package com.xiaohe.ScheduledTask.admin.core.model;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author : 小何
 * @date : 2023-08-22 16:52
 * @Description : 执行器组类
 * 将执行相同定时任务的多个执行器(服务器IP)封装到这个对象中。
 */
public class ScheduledTaskGroup {
    private int id;

    /**
     * 执行器中配置的项目名称
     */
    private String appname;

    /**
     * 中文名
     */
    private String title;

    /**
     * 执行器的注册方式
     * 0 : 自动注册，执行器向调度中心发送消息注册
     * 1 : 在web界面中手动录入
     */
    private int addressType;

    /**
     * 所有执行器的IP，以" ，" 隔开
     * 192.168.101.48,192.168.123.23,192.168.102.52
     */
    private String addressList;

    /**
     * 所有执行器IP的集合，将addressList转为List形式得到
     */
    private List<String> registryList;

    /**
     * 此执行器组的更新时间
     */
    private Date updateTime;

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

    public List<String> getRegistryList() {
        if (Objects.isNull(addressList) || addressList.trim().isEmpty()) {
            return null;
        }
        List<String> list = Arrays.asList(addressList.split(","));
        return list;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
