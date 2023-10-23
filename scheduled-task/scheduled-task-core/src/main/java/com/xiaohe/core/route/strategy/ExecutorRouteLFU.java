package com.xiaohe.core.route.strategy;

import com.xiaohe.core.route.ExecutorRouter;
import com.xiaohe.core.model.Result;
import com.xiaohe.core.model.TriggerParam;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : 小何
 * @Description : LFU
 * @date : 2023-10-09 15:44
 */
public class ExecutorRouteLFU extends ExecutorRouter {
    private static volatile long CACHE_TIME = 0;

    /**
     * key : jobId
     * value : HashMap : key : address
     *                   value : use count
     */
    private static ConcurrentHashMap<Integer, HashMap<String, Integer>> lfuMap = new ConcurrentHashMap<>();
    @Override
    public Result<String> route(TriggerParam triggerParam, List<String> addressList) {
        if (System.currentTimeMillis() >= CACHE_TIME) {
            CACHE_TIME = System.currentTimeMillis() + (1000 * 60 * 60 * 24);
            lfuMap.clear();
        }
        // key : address
        // value : use count
        HashMap<String, Integer> lfu = lfuMap.get(triggerParam.getJobId());
        if (lfu == null) {
            lfu = new HashMap<>();
            lfuMap.putIfAbsent(triggerParam.getJobId(), lfu);
        }
        synchronousData(lfu, addressList);

        return null;
    }

    /**
     * 将新老数据同步, lfu中的数据是老数据，要跟addressList中的数据同步
     * @param lfu 老数据
     * @param addressList 新数据
     */
    private void synchronousData(HashMap<String, Integer> lfu, List<String> addressList) {
        // addressList中有，lfu中没有的
        for (String address : addressList) {
            if (!lfu.containsKey(address) || lfu.get(address) >= 1000000) {
                int random = new Random().nextInt(addressList.size());
                lfu.put(address, random);
            }
        }
        // addressList中没有，lfu中有的
        Iterator<Map.Entry<String, Integer>> iterator = lfu.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Integer> entry = iterator.next();
            String address = entry.getKey();
            if (!addressList.contains(address)) {
                iterator.remove();
            }
        }

    }
}
