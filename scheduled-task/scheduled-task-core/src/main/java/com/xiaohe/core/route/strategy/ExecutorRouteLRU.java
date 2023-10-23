package com.xiaohe.core.route.strategy;

import com.xiaohe.core.route.ExecutorRouter;
import com.xiaohe.core.model.Result;
import com.xiaohe.core.model.TriggerParam;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : 小何
 * @Description : 最近最少使用
 * @date : 2023-10-09 15:23
 */
public class ExecutorRouteLRU extends ExecutorRouter {

    private static volatile long CACHE_TIME = 0;

    /**
     * key : jobId
     * value : LinkedHashMap. key : address
     *                        value : address
     */
    private static ConcurrentHashMap<Integer, LinkedHashMap<String, String>> lruMap = new ConcurrentHashMap<>();

    @Override
    public Result<String> route(TriggerParam triggerParam, List<String> addressList) {
        if (System.currentTimeMillis() >= CACHE_TIME) {
            CACHE_TIME = System.currentTimeMillis() + (1000 * 60 * 60 * 24);
            lruMap.clear();
        }
        LinkedHashMap<String, String> lru = lruMap.get(triggerParam.getJobId());
        if (lru == null) {
            lru = new LinkedHashMap<>(16, 0.75f, true);
            lruMap.putIfAbsent(triggerParam.getJobId(), lru);
        }
        // 同步传入的新address和已有的旧address
        synchronousData(lru, addressList);
        String address = lru.entrySet().iterator().next().getKey();
        return new Result<>(address);
    }

    private void synchronousData(LinkedHashMap<String, String> lru, List<String> addressList) {
        // 新增，addressList中有，但是lru中没有的需要加进去
        for (String address : addressList) {
            if (!lru.containsKey(address)) {
                lru.put(address, address);
            }
        }
        // oldAddress指 lru中有，但是addressList中没有的。需要删除
        Iterator<Map.Entry<String, String>> iterator = lru.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            String address = entry.getKey();
            if (!addressList.contains(address)) {
                iterator.remove();
            }
        }
     }
}
