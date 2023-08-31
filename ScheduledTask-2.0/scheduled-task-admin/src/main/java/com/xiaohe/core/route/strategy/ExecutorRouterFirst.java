package com.xiaohe.core.route.strategy;

import com.xiaohe.biz.model.Result;
import com.xiaohe.biz.model.TriggerParam;
import com.xiaohe.core.route.ExecutorRouter;

import java.util.List;

/**
 * @author : 小何
 * @Description : 优先第一个IP地址
 * @date : 2023-08-31 20:04
 */
public class ExecutorRouterFirst extends ExecutorRouter {

    @Override
    public Result<String> route(TriggerParam triggerParam, List<String> addressList) {
        String ip = addressList.get(0);
        return new Result<String>(ip);
    }
}
