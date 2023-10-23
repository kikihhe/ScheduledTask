package com.xiaohe.core.route.strategy;

import com.xiaohe.core.route.ExecutorRouter;
import com.xiaohe.core.model.Result;
import com.xiaohe.core.model.TriggerParam;

import java.util.List;

/**
 * @author : 小何
 * @Description : 选取最后一个
 * @date : 2023-10-09 10:49
 */
public class ExecutorRouteLast extends ExecutorRouter {

    @Override
    public Result<String> route(TriggerParam triggerParam, List<String> addressList) {
        return new Result<>(addressList.get(addressList.size() - 1));
    }
}
