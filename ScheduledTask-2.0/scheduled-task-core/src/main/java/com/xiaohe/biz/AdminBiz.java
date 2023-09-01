package com.xiaohe.biz;

import com.xiaohe.biz.model.HandleCallbackParam;
import com.xiaohe.biz.model.RegistryParam;
import com.xiaohe.biz.model.Result;

import java.util.List;

/**
 * @author : 小何
 * @Description : 执行器给调度中心发送消息的接口, 主要功能: 回调、注册执行器、删除执行器
 * @date : 2023-09-01 18:21
 */
public interface AdminBiz {

    /**
     * 回调给调度中心信息
     * @param callbackParamList
     * @return
     */
    public Result<String> callback(List<HandleCallbackParam> callbackParamList);

    /**
     * 执行器的注册
     * @param registryParam
     * @return
     */
    public Result<String> registry(RegistryParam registryParam) ;

    /**
     * 删除某执行器
     * @param registryParam
     * @return
     */
    public Result<String> registryRemove(RegistryParam registryParam);


}
