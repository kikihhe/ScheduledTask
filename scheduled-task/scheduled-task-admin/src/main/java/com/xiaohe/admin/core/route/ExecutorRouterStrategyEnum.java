package com.xiaohe.admin.core.route;

import com.xiaohe.admin.core.route.strategy.*;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-10-09 16:08
 */
public enum ExecutorRouterStrategyEnum {
    FIRST("jobconf_route_first", new ExecutorRouteFirst()),
    LAST("jobconf_route_last", new ExecutorRouteLast()),
    RANDOM("jobconf_route_random", new ExecutorRouteRandom()),
    ROUND("jobconf_route_round", new ExecutorRouteRound()),
    LEAST_FREQUENTLY_USED("jobconf_route_lfu", new ExecutorRouteLFU()),
    LEAST_RECENTLY_USED("jobconf_route_lru", new ExecutorRouteLRU()),
    FAILOVER("jobconf_route_failover", new ExecutorRouteFailover()),
    BUSYOVER("jobconf_route_busyover", new ExecutorRouteBusyOver()),
    SHARDING_BROADCAST("jobconf_route_sharding",  null)
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
