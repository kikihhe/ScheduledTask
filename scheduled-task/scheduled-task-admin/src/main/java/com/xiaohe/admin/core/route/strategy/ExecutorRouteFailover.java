package com.xiaohe.admin.core.route.strategy;

import com.xiaohe.admin.core.route.ExecutorRouter;
import com.xiaohe.admin.core.scheduler.XxlJobScheduler;
import com.xiaohe.core.biz.ExecutorBiz;
import com.xiaohe.core.model.Result;
import com.xiaohe.core.model.TriggerParam;

import java.util.List;

/**
 * @author : 小何
 * @Description : 故障转移
 * @date : 2023-10-09 11:20
 */
public class ExecutorRouteFailover extends ExecutorRouter {
    @Override
    public Result<String> route(TriggerParam triggerParam, List<String> addressList) {
        StringBuffer result = new StringBuffer();
        for (String address : addressList) {
            Result beatResult = null;
            try {
                ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(address);
                beatResult = executorBiz.beat();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                beatResult = Result.error("" + e);
            }
            result.append(result.length() > 0 ? "<br></br>" : "")
                    .append("jobconf_beat：")
                    .append("<br>address：" + address)
                    .append("<br>code：" + beatResult.getCode())
                    .append("<br>msg：" + beatResult.getMessage());
            if (beatResult.getCode() == Result.SUCCESS_CODE) {
                beatResult.setMessage(result.toString());
                beatResult.setContent(address);
                return beatResult;
            }
        }
        return Result.error(result.toString());
    }
}
