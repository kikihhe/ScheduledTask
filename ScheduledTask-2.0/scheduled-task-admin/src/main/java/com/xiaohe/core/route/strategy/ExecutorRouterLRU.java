package com.xiaohe.core.route.strategy;

import com.xiaohe.biz.model.Result;
import com.xiaohe.biz.model.TriggerParam;
import com.xiaohe.core.route.ExecutorRouter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : 小何
 * @Description : LRU算法挑选
 * @date : 2023-08-31 21:23
 */
public class ExecutorRouterLRU extends ExecutorRouter {

    private static final ConcurrentHashMap<Integer, LinkedHashMap<String, String>> lru = new ConcurrentHashMap<>();

    /**
     * 缓存到期时间
     */
    private static long CACHE_VALID_TIME = 0;

    public String route(int jobId, List<String> addressList) {
        // 如果过期，刷新
        if (System.currentTimeMillis() >= CACHE_VALID_TIME) {
            lru.clear();
            CACHE_VALID_TIME = System.currentTimeMillis() + 24 * 60 * 60 * 1000;
        }
        // 取出 jobId 对应的 LRU链表(内含执行器地址), 如果LRU链表为空就创建
        LinkedHashMap<String, String> lruItem = lru.get(jobId);
        if (lruItem == null) {
            lruItem = new LinkedHashMap<>(16, 0.75f, true);
            lru.putIfAbsent(jobId, lruItem);
        }
        // 将LRU链表中的IP地址 与 传入的最新IP地址做同步
        // 增lruItem
        for (String address : addressList) {
            if (!lruItem.containsKey(address)) {
                lruItem.putIfAbsent(address, address);
            }
        }
        // 删lruItem
        List<String> deleteAddress = new ArrayList<>();
        for (String address : lruItem.keySet()) {
            if (!addressList.contains(address)) {
                deleteAddress.add(address);
            }
        }
        for (String address : deleteAddress) {
            lruItem.remove(address);
        }

        // 开始挑选
        String address = lruItem.entrySet().iterator().next().getKey();
        return address;
    }


    @Override
    public Result<String> route(TriggerParam triggerParam, List<String> addressList) {
        String address = route(triggerParam.getJobId(), addressList);
        return new Result<String>(address);
    }
}
