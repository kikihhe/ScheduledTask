package com.xiaohe.handler;

/**
 * @author : 小何
 * @Description : JobHandler, 初始化、销毁、执行方法
 * @date : 2023-09-08 11:05
 */
public abstract class IJobHandler {
    public abstract void executor() throws Exception;


    public void init() throws Exception {

    }

    public void destroy() throws Exception {

    }

}
