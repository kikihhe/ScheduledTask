package com.xiaohe.ScheduledTask.core.handler;

/**
 * @author : 小何
 * @Description : 封装定时任务方法的接口
 * @date : 2023-08-24 15:21
 */
public abstract class IJobHandler {
    public abstract void execute() throws Exception;


    public void init() throws Exception {

    }


    public void destroy() throws Exception {

    }
}
