package com.xiaohe.core.route.strategy;

import com.xiaohe.biz.model.Result;
import com.xiaohe.biz.model.TriggerParam;
import com.xiaohe.core.route.ExecutorRouter;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author : 小何
 * @Description : 轮询
 * @date : 2023-08-31 20:58
 */
public class ExecutorRouterRound extends ExecutorRouter {
    /**
     * 记录每一个任务轮询到哪个下标了
     * key : 定时任务id
     * value : 下标
     */
    private static ConcurrentHashMap<Integer, AtomicInteger> roundCount = new ConcurrentHashMap<>();

    /**
     * 缓存到期时间
     */
    private static long CACHE_VALID_TIME = 0;

    private static Random random = new Random();

    private static int getIndex(int jobId) {
        // 如果过期了，就重置所有下标，重新计算
        if (System.currentTimeMillis() >= CACHE_VALID_TIME) {
            roundCount.clear();
            CACHE_VALID_TIME = System.currentTimeMillis() + 24 * 60 * 60 * 1000;
        }
        AtomicInteger count = roundCount.get(jobId);
        // 如果count为空，证明还没有轮询过这个任务，将其添加到Map中。
        // 如果轮询次数大于10000，重新计算
        // 否则直接加，然后返回对应值
        if (count == null || count.get() >= 1000000) {
            count = new AtomicInteger(random.nextInt(1000));
        } else {
            count.addAndGet(1);
        }
        roundCount.put(jobId, count);
        return count.get();

    }

    @Override
    public Result<String> route(TriggerParam triggerParam, List<String> addressList) {
        int index = getIndex(triggerParam.getJobId()) % addressList.size();
        return new Result<String>(addressList.get(index));
    }
}
