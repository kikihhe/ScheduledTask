package com.xiaohe.core.route;

/**
 * @author : 小何
 * @Description : 调度策略的枚举类
 * @date : 2023-08-31 20:00
 */
public enum ExecutorRouteStrategyEnum {
    ;
    /**
     * 调度策略
     */
    private String title;

    /**
     * 具体的实现类
     */
    private ExecutorRouter router;

    ExecutorRouteStrategyEnum(String title, ExecutorRouter router) {
        this.title = title;
        this.router = router;
    }

    /**
     * 用于判断选择的调度策略是否存在。
     * @param name
     * @param defaultItem
     * @return
     */
    public static ExecutorRouteStrategyEnum match(String name, ExecutorRouteStrategyEnum defaultItem) {
        if (name != null) {
            for (ExecutorRouteStrategyEnum item : ExecutorRouteStrategyEnum.values()) {
                if (item.name().equals(name)) {
                    return item;
                }
            }
        }
        return defaultItem;
    }













    public String getTitle() {
        return title;
    }

    public ExecutorRouter getRouter() {
        return router;
    }



}
