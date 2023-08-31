package com.xiaohe.core.route.strategy;

import com.xiaohe.biz.model.Result;
import com.xiaohe.biz.model.TriggerParam;
import com.xiaohe.core.route.ExecutorRouter;

import java.util.List;

/**
 * @author : 小何
 * @Description : 优先调度最后一个
 * @date : 2023-08-31 20:06
 */
public class ExecutorRouterLast extends ExecutorRouter {
    @Override
    public Result<String> route(TriggerParam triggerParam, List<String> addressList) {
        return new Result<String>(addressList.get(addressList.size() - 1));
    }
}
