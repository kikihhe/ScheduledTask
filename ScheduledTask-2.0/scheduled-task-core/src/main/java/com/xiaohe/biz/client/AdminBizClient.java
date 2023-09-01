package com.xiaohe.biz.client;

import com.xiaohe.biz.AdminBiz;
import com.xiaohe.biz.model.HandleCallbackParam;
import com.xiaohe.biz.model.RegistryParam;
import com.xiaohe.biz.model.Result;
import com.xiaohe.util.RemotingUtil;

import java.util.List;

/**
 * @author : 小何
 * @Description : 执行器给调度中心发送消息的客户端
 * @date : 2023-09-01 18:33
 */
public class AdminBizClient implements AdminBiz {

    /**
     * 调度中心可能是集群，所以要指定url
     */
    private String addressUrl;

    /**
     * token令牌，执行器和调度中心要一致
     */
    private String accessToken;

    /**
     * 通信超时时间
     */
    private int timeout = 3;


    @Override
    public Result<String> callback(List<HandleCallbackParam> callbackParamList) {
        return RemotingUtil.postBody(addressUrl + "api/callback", accessToken, timeout, callbackParamList, String.class);
    }

    @Override
    public Result<String> registry(RegistryParam registryParam) {
        return RemotingUtil.postBody(addressUrl + "api/registry", accessToken, timeout, registryParam, String.class);
    }

    @Override
    public Result<String> registryRemove(RegistryParam registryParam) {
        return RemotingUtil.postBody(addressUrl + "api/registryRemove", accessToken, timeout, registryParam, String.class);
    }

    public AdminBizClient(String addressUrl, String accessToken) {
        this.addressUrl = addressUrl;
        this.accessToken = accessToken;
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

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
