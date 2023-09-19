package com.xiaohe.biz;

import com.xiaohe.biz.model.LogParam;
import com.xiaohe.biz.model.Result;
import com.xiaohe.biz.model.TriggerParam;

import com.xiaohe.biz.model.IdleBeatParam;

/**
 * @author : 小何
 * @Description : 用于远程调用的客户端接口，
 * ExecutorBizClient 是调度中心给执行器发送消息的接口
 * ExecutorBizImpl 是执行器用于处理 从调度中心接收到的信息 的实现类
 * @date : 2023-09-01 10:42
 */
public interface ExecutorBiz {

    public Result<String> beat();



    public Result<String> idleBeat(IdleBeatParam idleBeatParam);


    public Result<String> run(TriggerParam triggerParam);



    public Result<String> log(LogParam logParam);

    public Result<String> kill(TriggerParam triggerParam);

}
