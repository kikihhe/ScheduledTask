package com.xiaohe.core.route.strategy;

import com.xiaohe.core.route.ExecutorRouter;
import com.xiaohe.admin.core.scheduler.XxlJobScheduler;
import com.xiaohe.admin.core.util.I18nUtil;
import com.xiaohe.core.biz.ExecutorBiz;
import com.xiaohe.core.model.IdleBeatParam;
import com.xiaohe.core.model.Result;
import com.xiaohe.core.model.TriggerParam;

import java.util.List;

/**
 * @author : 小何
 * @Description : 忙碌检测
 * @date : 2023-10-09 12:43
 */
public class ExecutorRouteBusyOver extends ExecutorRouter {
    @Override
    public Result<String> route(TriggerParam triggerParam, List<String> addressList) {
        StringBuffer result = new StringBuffer();
        for (String address : addressList) {
            Result beatResult = null;
            try {
                ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(address);
                beatResult = executorBiz.idleBeat(new IdleBeatParam(triggerParam.getJobId()));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                beatResult = Result.error("" + e);
            }
            result.append((result.length() > 0) ? "<br><br>" : "")
                    .append(I18nUtil.getString("jobconf_idleBeat") + "：")
                    .append("<br>address：").append(address)
                    .append("<br>code：").append(beatResult.getCode())
                    .append("<br>msg：").append(beatResult.getMessage());
            if (beatResult.getCode() == Result.SUCCESS_CODE) {
                beatResult.setMessage(result.toString());
                beatResult.setContent(address);
                return beatResult;
            }
        }
        return Result.error(result.toString());
    }
}
