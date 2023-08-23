package com.xiaohe.ScheduledTask.admin.core.model;

import java.util.Date;

/**
 * @author : 小何
 * @Description : 调度中心持有注册过来的执行器的实体类
 * @date : 2023-08-22 17:01
 */
public class ScheduledTaskRegistry {
    /**
     * 执行器id
     */
    private int id;

    /**
     * 执行器的注册方法，手动/自动
     */
    private String registryGroup;

    /**
     * 任务名称
     */
    private String registryKey;

    /**
     * 执行器的地址
     */
    private String registryValue;

    /**
     * 更新时间
     */
    private Date updateTime;



}
