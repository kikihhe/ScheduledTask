package com.xiaohe.admin.core.route.strategy;

import com.xiaohe.admin.core.route.ExecutorRouter;
import com.xiaohe.core.model.Result;
import com.xiaohe.core.model.TriggerParam;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author : 小何
 * @Description : 轮询
 * @date : 2023-10-09 10:53
 */
public class ExecutorRouteRound extends ExecutorRouter {

    /**
     * 使用Map记录对应的任务轮询到哪个下标了
     * key : 任务id
     * value : 轮到哪个下标了
     */
    private static ConcurrentHashMap<Integer, AtomicInteger> roundMap = new ConcurrentHashMap<>();

    /**
     * 缓存时间，Map中的值一天清空一次
     */
    private static long CACHE_TIME = 0;

    @Override
    public Result<String> route(TriggerParam triggerParam, List<String> addressList) {
        if (System.currentTimeMillis() >= CACHE_TIME) {
            CACHE_TIME = System.currentTimeMillis() + (1000 * 60 * 60 * 24);
            roundMap.clear();
        }
        int index = getIndex(triggerParam) % addressList.size();
        return new Result<>(addressList.get(index));
    }

    private int getIndex(TriggerParam triggerParam) {
        AtomicInteger atomicInteger = roundMap.get(triggerParam.getJobId());
        // 如果为空，说明这个任务还没有执行过
        // 如果太大，就干脆重新执行。
        if (atomicInteger == null || atomicInteger.get() >= 10000) {
            // 为什么要生成随机数，也就是为啥一个任务的第一次路由策略需要随机路由？
            // 害怕多个任务路由到同一个执行器，导致这个执行器压力太大。
            int random = new Random().nextInt(100);
            atomicInteger = new AtomicInteger(random);
        } else {
            atomicInteger.incrementAndGet();
        }
        roundMap.put(triggerParam.getJobId(), atomicInteger);
        return atomicInteger.get();
    }
}
