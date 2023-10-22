package com.xiaohe.admin.service.impl;

import com.xiaohe.admin.core.thread.JobCompleteHelper;
import com.xiaohe.admin.core.thread.JobRegistryHelper;
import com.xiaohe.core.biz.AdminBiz;
import com.xiaohe.core.model.HandlerCallbackParam;
import com.xiaohe.core.model.RegistryParam;
import com.xiaohe.core.model.Result;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author : 小何
 * @Description : 由JobApiController调用，处理从
 * @date : 2023-10-14 19:11
 */
@Service
public class AdminBizImpl implements AdminBiz {
    @Override
    public Result callback(List<HandlerCallbackParam> callbackParamList) {
        return JobCompleteHelper.getInstance().callback(callbackParamList);
    }

    @Override
    public Result registry(RegistryParam registryParam) {
        return JobRegistryHelper.getInstance().registry(registryParam);
    }

    @Override
    public Result registryRemove(RegistryParam registryParam) {
        return JobRegistryHelper.getInstance().registryRemove(registryParam);
    }
}
