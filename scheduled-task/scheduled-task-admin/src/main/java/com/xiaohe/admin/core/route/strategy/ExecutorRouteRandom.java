package com.xiaohe.admin.core.route.strategy;

import com.xiaohe.admin.core.route.ExecutorRouter;
import com.xiaohe.core.model.Result;
import com.xiaohe.core.model.TriggerParam;

import java.util.List;
import java.util.Random;

/**
 * @author : 小何
 * @Description : 随机选取
 * @date : 2023-10-09 10:51
 */
public class ExecutorRouteRandom extends ExecutorRouter {

    @Override
    public Result<String> route(TriggerParam triggerParam, List<String> addressList) {
        int random = new Random().nextInt(addressList.size());
        return new Result<>(addressList.get(random));
    }
}
