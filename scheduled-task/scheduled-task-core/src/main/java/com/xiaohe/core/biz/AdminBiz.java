package com.xiaohe.core.biz;

import com.xiaohe.core.model.HandlerCallbackParam;
import com.xiaohe.core.model.RegistryParam;
import com.xiaohe.core.model.Result;

import java.util.List;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-09-29 15:26
 */
public interface AdminBiz {

    public Result callback(List<HandlerCallbackParam> callbackParamList);

    public Result registry(RegistryParam registryParam);

    public Result registryRemove(RegistryParam registryParam);
}
