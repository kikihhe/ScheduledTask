package com.xiaohe.ScheduledTask.core.biz.client;

import com.xiaohe.ScheduledTask.core.biz.AdminBiz;
import com.xiaohe.ScheduledTask.core.biz.model.RegistryParam;
import com.xiaohe.ScheduledTask.core.biz.model.Result;
import com.xiaohe.ScheduledTask.core.util.RemotingUtil;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-08-24 20:55
 */
public class AdminBizClient implements AdminBiz {
    /**
     * 调度中心的url
     */
    private String addressUrl;

    /**
     * token
     */
    private String accessToken;

    /**
     * 访问超时时间
     */
    private int timeout = 3;

    public AdminBizClient() {
    }

    public AdminBizClient(String addressUrl, String accessToken) {
        this.addressUrl = addressUrl;
        this.accessToken = accessToken;
    }

    @Override
    public Result registry(RegistryParam registryParam) {
        // 调用工具类发送http post请求，将执行器注册到指定url的调度中心上。
        return RemotingUtil.postBody(addressUrl + "api/registry", accessToken, timeout, registryParam, String.class);
    }

    @Override
    public Result registryRemove(RegistryParam registryParam) {
        // 调用工具类发送http post请求，将执行器从指定的调度中心那里删除
        return RemotingUtil.postBody(addressUrl + "api/registryRemove", accessToken, timeout, registryParam, String.class);
    }

    public String getAddressUrl() {
        return addressUrl;
    }

    public void setAddressUrl(String addressUrl) {
        this.addressUrl = addressUrl;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
