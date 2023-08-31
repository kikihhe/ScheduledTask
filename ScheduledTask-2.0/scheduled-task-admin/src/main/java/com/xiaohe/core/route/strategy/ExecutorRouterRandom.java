package com.xiaohe.core.route.strategy;

import com.xiaohe.biz.model.Result;
import com.xiaohe.biz.model.TriggerParam;
import com.xiaohe.core.route.ExecutorRouter;

import java.util.List;
import java.util.Random;

/**
 * @author : 小何
 * @Description : 随机挑选
 * @date : 2023-08-31 20:06
 */
public class ExecutorRouterRandom extends ExecutorRouter {
    private static final Random random = new Random();


    @Override
    public Result<String> route(TriggerParam triggerParam, List<String> addressList) {
        int index = random.nextInt(addressList.size());
        return new Result<String>(addressList.get(index));
    }
}
