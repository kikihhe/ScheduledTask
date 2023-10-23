package com.xiaohe.admin.core.route.strategy;

import com.xiaohe.admin.core.route.ExecutorRouter;
import com.xiaohe.core.model.Result;
import com.xiaohe.core.model.TriggerParam;

import java.util.List;

/**
 * @author : 小何
 * @Description : 选取第一个
 * @date : 2023-10-09 10:48
 */
public class ExecutorRouteFirst extends ExecutorRouter {
    /**
     * 选取第一个
     * @param triggerParam
     * @param addressList
     * @return
     */
    @Override
    public Result<String> route(TriggerParam triggerParam, List<String> addressList) {
        return new Result<>(addressList.get(0));
    }
}
