package com.xiaohe.core.route;

import com.xiaohe.biz.model.Result;
import com.xiaohe.biz.model.TriggerParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-08-31 19:47
 */
public abstract class ExecutorRouter {
    protected static Logger logger = LoggerFactory.getLogger(ExecutorRouter.class);

    /**
     * 获取IP地址
     * @param triggerParam
     * @param addressList
     * @return
     */
    public abstract Result<String> route(TriggerParam triggerParam, List<String> addressList);
}
