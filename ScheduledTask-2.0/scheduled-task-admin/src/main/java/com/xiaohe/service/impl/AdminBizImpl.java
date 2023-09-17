package com.xiaohe.service.impl;

import com.xiaohe.biz.AdminBiz;
import com.xiaohe.biz.model.HandleCallbackParam;
import com.xiaohe.biz.model.RegistryParam;
import com.xiaohe.biz.model.Result;
import com.xiaohe.core.thread.JobCompleteHelper;
import com.xiaohe.core.thread.JobRegistryHelper;

import java.util.List;

/**
 * @author : 小何
 * @Description : 调度中心对于从执行器接收的消息，做出具体处理的类（调用JobCompleteHelper、JobRegistryHelper做处理）
 * @date : 2023-09-05 13:40
 */
public class AdminBizImpl implements AdminBiz {
    @Override
    public Result<String> callback(List<HandleCallbackParam> callbackParamList) {
        return JobCompleteHelper.getInstance().callback(callbackParamList);
    }

    @Override
    public Result<String> registry(RegistryParam registryParam) {
        return JobRegistryHelper.getInstance().registry(registryParam);
    }

    @Override
    public Result<String> registryRemove(RegistryParam registryParam) {
        return JobRegistryHelper.getInstance().registryRemove(registryParam);
    }
}
