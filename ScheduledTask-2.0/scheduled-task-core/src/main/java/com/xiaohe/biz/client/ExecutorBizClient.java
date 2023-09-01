package com.xiaohe.biz.client;

import com.xiaohe.biz.ExecutorBiz;
import com.xiaohe.biz.model.IdleBeatParam;
import com.xiaohe.biz.model.LogParam;
import com.xiaohe.biz.model.Result;
import com.xiaohe.biz.model.TriggerParam;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-09-01 10:52
 */
public class ExecutorBizClient implements ExecutorBiz {
    /**
     * 执行器地址
     */
    private String addressUrl;

    /**
     * 通信token
     */
    private String accessToken;

    /**
     * 超时时间
     */
    private int timeout = 3;


    /**
     * 调度中心发送给执行器用于心跳检测
     * @return
     */
    @Override
    public Result<String> beat() {
        return null;
    }

    /**
     * 调度中心使用 忙碌转移 策略进行执行器的负载均衡时会给每一个执行器发送一个idleBeat消息      <br></br>
     * 用于检测这个执行器是否正在执行这个定时任务(是否正在忙碌)
     *
     * @param idleBeatParam 内含定时任务id
     */
    @Override
    public Result<String> idleBeat(IdleBeatParam idleBeatParam) {
        return null;
    }

    /**
     * 调度中心发送给执行器用于调度某个任务的信息
     * @param triggerParam
     * @return
     */
    @Override
    public Result<String> run(TriggerParam triggerParam) {
        return null;
    }

    /**
     * 调度中心发送给执行器用于记录日志的信息
     * @param logParam
     * @return
     */
    @Override
    public Result<String> log(LogParam logParam) {
        return null;
    }

    public ExecutorBizClient(String addressUrl, String accessToken) {
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
