package com.xiaohe.core.biz.client;

import com.xiaohe.core.biz.ExecutorBiz;
import com.xiaohe.core.handler.annotation.XxlJob;
import com.xiaohe.core.model.*;
import com.xiaohe.core.util.XxlJobRemotingUtil;

/**
 * @author : 小何
 * @Description : 给执行器发送消息的客户端，调度中心使用
 * @date : 2023-10-05 19:46
 */
public class ExecutorBizClient implements ExecutorBiz {

    /**
     * 目标执行器的url
     */
    private String addressUrl;

    /**
     * token
     */
    private String accessToken;

    /**
     * 请求超时时间
     */
    private int timeout = 3;

    public ExecutorBizClient() {
    }
    public ExecutorBizClient(String addressUrl, String accessToken) {
        this.addressUrl = addressUrl;
        this.accessToken = accessToken;
    }


    @Override
    public Result beat() {
        return XxlJobRemotingUtil.postBody(addressUrl + "beat", accessToken, timeout, "", String.class);
    }

    @Override
    public Result idleBeat(IdleBeatParam idleBeatParam) {
        return XxlJobRemotingUtil.postBody(addressUrl + "idleBeat", accessToken, timeout, idleBeatParam, String.class);
    }

    @Override
    public Result run(TriggerParam triggerParam) {
        return XxlJobRemotingUtil.postBody(addressUrl + "run", accessToken, timeout, triggerParam, String.class);
    }

    @Override
    public Result kill(KillParam killParam) {
        return XxlJobRemotingUtil.postBody(addressUrl + "kill", accessToken, timeout, killParam, String.class);
    }

    @Override
    public Result log(LogParam logParam) {
        return XxlJobRemotingUtil.postBody(addressUrl + "log", accessToken, timeout, logParam, LogResult.class);
    }
}
