package com.xiaohe.ScheduledTask.core.biz;

import com.xiaohe.ScheduledTask.core.biz.model.RegistryParam;
import com.xiaohe.ScheduledTask.core.biz.model.Result;

/**
 * @author : 小何
 * @Description : 执行器用于给调度中心发送消息的客户端
 * @date : 2023-08-24 20:52
 */
public interface AdminBiz {
    /**
     * 执行器向调度中心注册
     * @param registryParam
     * @return
     */
    public Result<String> registry(RegistryParam registryParam);


    /**
     * 执行器向调度中心发送消息，将自己移除
     * @param registryParam
     * @return
     */
    public Result<String> registryRemove(RegistryParam registryParam);

}
