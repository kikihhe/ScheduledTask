package com.xiaohe.core.route.strategy;

import com.xiaohe.biz.ExecutorBiz;
import com.xiaohe.biz.model.IdleBeatParam;
import com.xiaohe.biz.model.Result;
import com.xiaohe.biz.model.TriggerParam;
import com.xiaohe.core.route.ExecutorRouteStrategyEnum;
import com.xiaohe.core.route.ExecutorRouter;
import com.xiaohe.core.scheduler.TaskScheduler;
import com.xiaohe.core.util.I18nUtil;

import java.util.List;

/**
 * @author : 小何
 * @Description : 忙碌转移
 * @date : 2023-09-01 19:26
 */
public class ExecutorRouterBusyOver extends ExecutorRouter {

    @Override
    public Result<String> route(TriggerParam triggerParam, List<String> addressList) {
        StringBuffer idleBeatResult = new StringBuffer();
        // 遍历执行器，向执行器询问是否忙碌
        for (String address : addressList) {
            Result<String> result = null;
            try {
                ExecutorBiz executorBiz = TaskScheduler.getExecutorBiz(address);
                result = executorBiz.idleBeat(new IdleBeatParam(triggerParam.getJobId()));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            idleBeatResult.append( (idleBeatResult.length()>0)?"<br><br>":"")
                    .append(I18nUtil.getString("jobconf_idleBeat") + "：")
                    .append("<br>address：").append(address)
                    .append("<br>code：").append(result.getCode())
                    .append("<br>msg：").append(result.getMessage());

            // 如果不忙碌就使用它
            if (result.getCode() == Result.SUCCESS_CODE) {
                result.setMessage(idleBeatResult.toString());
                result.setContent(address);
                return result;
            }

        }
        // 遍历完，所有的执行器都忙碌，转为随机挑选策略
        return ExecutorRouteStrategyEnum.RANDOM.getRouter().route(triggerParam, addressList);
    }
}
