package com.xiaohe.core.handler;

/**
 * @author : 小何
 * @Description : 封装定时任务执行方法的接口
 * @date : 2023-09-30 20:55
 */
public abstract class IJobHandler {

    public abstract void execute() throws  Exception;

    public void init() throws  Exception {

    }

    public void destroy() throws Exception {

    }
}
