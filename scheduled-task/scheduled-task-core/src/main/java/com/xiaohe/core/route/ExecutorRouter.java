package com.xiaohe.core.route;

import com.xiaohe.core.model.Result;
import com.xiaohe.core.model.TriggerParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author : 小何
 * @Description : 调度中心使用路由策略选择执行器组中的一个执行器。
 * @date : 2023-10-09 10:45
 */
public abstract class ExecutorRouter {
    protected static Logger logger = LoggerFactory.getLogger(ExecutorRouter.class);

    public abstract Result<String> route(TriggerParam triggerParam, List<String> addressList);
}
