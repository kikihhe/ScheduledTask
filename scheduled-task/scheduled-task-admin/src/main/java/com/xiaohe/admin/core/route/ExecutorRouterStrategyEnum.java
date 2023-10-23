package com.xiaohe.admin.core.route;

import com.xiaohe.admin.core.route.strategy.*;
import com.xiaohe.admin.core.util.I18nUtil;


/**
 * @author : 小何
 * @Description :
 * @date : 2023-10-09 16:08
 */
public enum ExecutorRouterStrategyEnum {
    //使用第一个
    FIRST(I18nUtil.getString("jobconf_route_first"), new ExecutorRouteFirst()),
    //使用最后一个
    LAST(I18nUtil.getString("jobconf_route_last"), new ExecutorRouteLast()),
    //轮训
    ROUND(I18nUtil.getString("jobconf_route_round"), new ExecutorRouteRound()),
    //随机
    RANDOM(I18nUtil.getString("jobconf_route_random"), new ExecutorRouteRandom()),
    //最不经常使用
    LEAST_FREQUENTLY_USED(I18nUtil.getString("jobconf_route_lfu"), new ExecutorRouteLFU()),
    //最近最久未使用
    LEAST_RECENTLY_USED(I18nUtil.getString("jobconf_route_lru"), new ExecutorRouteLRU()),
    //故障转移
    FAILOVER(I18nUtil.getString("jobconf_route_failover"), new ExecutorRouteFailover()),
    //忙碌转移
    BUSYOVER(I18nUtil.getString("jobconf_route_busyover"), new ExecutorRouteBusyOver()),
    //分片广播
    SHARDING_BROADCAST(I18nUtil.getString("jobconf_route_shard"), null);
    ;
    private String title;

    private ExecutorRouter executorRouter;

    ExecutorRouterStrategyEnum(String title, ExecutorRouter executorRouter) {
        this.title = title;
        this.executorRouter = executorRouter;
    }

    public String getTitle() {
        return title;
    }

    public ExecutorRouter getExecutorRouter() {
        return executorRouter;
    }

    public static ExecutorRouterStrategyEnum match(String name, ExecutorRouterStrategyEnum defaultEnum) {
        for (ExecutorRouterStrategyEnum routeEnum : ExecutorRouterStrategyEnum.values()){
            if (routeEnum.getTitle().equals(name)) {
                return routeEnum;
            }
        }
        return defaultEnum;
    }
}
