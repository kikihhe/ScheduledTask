package com.xiaohe.core.route.strategy;

import com.xiaohe.biz.model.Result;
import com.xiaohe.biz.model.TriggerParam;
import com.xiaohe.core.route.ExecutorRouter;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : 小何
 * @Description : LFU 使用频率
 * @date : 2023-08-31 21:53
 */
public class ExecutorRouterLFU extends ExecutorRouter {
    /**
     * key : 任务id
     * value : key : 执行器地址
     * value : 该执行器使用过的次数
     */
    private static ConcurrentHashMap<Integer, HashMap<String, Integer>> lfu = new ConcurrentHashMap<>();

    /**
     * 缓存到期时间
     */
    private static long CACHE_VALID_TIME = 0;

    private static Random random = new Random();


    private String route(int jobId, List<String> addressList) {
        if (System.currentTimeMillis() >= CACHE_VALID_TIME) {
            lfu.clear();
            CACHE_VALID_TIME = System.currentTimeMillis() + 24 * 60 * 60 * 1000;
        }
        HashMap<String, Integer> hashMap = lfu.get(jobId);
        if (hashMap == null) {
            hashMap = new HashMap<>();
            lfu.putIfAbsent(jobId, hashMap);
        }
        // hashMap中的地址 与 传入的地址 做同步
        for (String address : addressList) {
            // 如果没有使用过 或者 用的太多了，重置使用次数
            if (!hashMap.containsKey(address) || hashMap.get(address) > 1000000) {
                hashMap.putIfAbsent(address, random.nextInt(addressList.size()));
            }
        }
        List<String> delete = new ArrayList<>();
        for (String address : hashMap.keySet()) {
            if (!addressList.contains(address)) {
                delete.add(address);
            }
        }
        for (String address : delete) {
            hashMap.remove(address);
        }
        // 开始挑选, 首先将 hashMap 中的所有IP按照使用次数排序
        ArrayList<Map.Entry<String, Integer>> list = new ArrayList<>(hashMap.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
        // 选择第一个，也就是使用频率最小的
        Map.Entry<String, Integer> entry = list.get(0);
        String address = entry.getKey();
        entry.setValue(entry.getValue() + 1);
        return address;
    }


    @Override
    public Result<String> route(TriggerParam triggerParam, List<String> addressList) {
        String route = route(triggerParam.getJobId(), addressList);
        return new Result<String>(route);
    }
}
