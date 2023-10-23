package com.xiaohe.core.biz.client;

import com.xiaohe.core.biz.AdminBiz;
import com.xiaohe.core.model.HandlerCallbackParam;
import com.xiaohe.core.model.RegistryParam;
import com.xiaohe.core.model.Result;
import com.xiaohe.core.model.XxlJobInfo;
import com.xiaohe.core.util.XxlJobRemotingUtil;

import java.util.List;

/**
 * @author : 小何
 * @Description : 执行器给调度中心发送消息的工具类
 * @date : 2023-09-29 15:28
 */
public class AdminBizClient implements AdminBiz {
    /**
     * 调度中心的地址
     */
    private String addressUrl;
    /**
     * 通信所需token
     */
    private String accessToken;

    /**
     * 如果调度中心长时间不回应，直接结束
     */
    private int timeout = 3;

    public AdminBizClient(String addressUrl, String accessToken) {
        this.addressUrl = addressUrl;
        this.accessToken = accessToken;
    }

    /**
     * 执行器将定时任务的执行结果回调给调度中心
     * @param callbackParamList
     * @return
     */
    @Override
    public Result callback(List<HandlerCallbackParam> callbackParamList) {
        return XxlJobRemotingUtil.postBody(addressUrl + "api/callback", accessToken, timeout, callbackParamList, String.class);
    }

    /**
     * 执行器注册与维持心跳都用这个方法
     * @param registryParam
     * @return
     */
    @Override
    public Result registry(RegistryParam registryParam) {
        return XxlJobRemotingUtil.postBody(addressUrl + "api/registry", accessToken, timeout, registryParam, String.class);
    }

    /**
     * 执行器注销
     * @param registryParam
     * @return
     */
    @Override
    public Result registryRemove(RegistryParam registryParam) {
        return XxlJobRemotingUtil.postBody(addressUrl + "api/registryRemove", accessToken, timeout, registryParam, String.class);
    }

    public Result registerMethod(List<XxlJobInfo> xxlJobInfos) {
        return XxlJobRemotingUtil.postBody(addressUrl + "jobinfo/addBatch", accessToken, timeout, xxlJobInfos, String.class);
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
