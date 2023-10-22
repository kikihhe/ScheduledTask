package com.xiaohe.admin.core.trigger;

import com.xiaohe.admin.core.conf.XxlJobAdminConfig;
import com.xiaohe.admin.core.model.XxlJobGroup;
import com.xiaohe.admin.core.model.XxlJobInfo;
import com.xiaohe.admin.core.model.XxlJobLog;
import com.xiaohe.admin.core.route.ExecutorRouterStrategyEnum;
import com.xiaohe.admin.core.scheduler.XxlJobScheduler;
import com.xiaohe.admin.core.util.I18nUtil;
import com.xiaohe.core.biz.ExecutorBiz;
import com.xiaohe.core.enums.ExecutorBlockStrategyEnum;
import com.xiaohe.core.model.Result;
import com.xiaohe.core.model.TriggerParam;
import com.xiaohe.core.util.CollectionUtil;
import com.xiaohe.core.util.IPUtil;
import com.xiaohe.core.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

/**
 * @author : 小何
 * @Description : 线程池接收任务后，交给这个组件。它会根据调度类型、路由策略来选择执行器 进行远程调度.
 * 在此处创建的 XxlJobLog、TriggerParam
 * @date : 2023-10-09 17:54
 */
public class XxlJobTrigger {
    private static Logger logger = LoggerFactory.getLogger(XxlJobTrigger.class);

    /**
     * 提供给外界调用
     *
     * @param jobId
     * @param triggerType
     * @param failRetryCount
     * @param executorShardingParam
     * @param executorParam
     * @param addressList
     */
    public static void trigger(int jobId,
                               TriggerTypeEnum triggerType,
                               int failRetryCount,
                               String executorShardingParam,
                               String executorParam,
                               String addressList) {
        XxlJobInfo jobInfo = XxlJobAdminConfig.getAdminConfig().getXxlJobInfoMapper().loadById(jobId);
        if (jobInfo == null) {
            logger.warn(">>>>>>>>>>> trigger fail, jobId invalid, jobId={}", jobId);
            return;
        }
        // 如果指定了，用指定的。没指定就用数据库的。
        if (executorParam != null) {
            jobInfo.setExecutorParam(executorParam);
        }
        int finalFailRetryCount = failRetryCount >= 0 ? failRetryCount : jobInfo.getExecutorFailRetryCount();
        XxlJobGroup jobGroup = XxlJobAdminConfig.getAdminConfig().getXxlJobGroupMapper().load(jobInfo.getJobGroup());
        if (StringUtil.hasText(addressList)) {
            jobGroup.setAddressList(addressList);
            jobGroup.setAddressType(1);
        }
        // 分片广播逻辑, 如果设置了分片广播，executorSharingParam不为空，取出两个参数: 分片索引、分片总数
        int[] shardingParam = null;
        if (executorShardingParam != null) {
            String[] shardingArr = executorShardingParam.split("/");
            if (shardingArr.length == 2 && isNumeric(shardingArr[0]) && isNumeric(shardingArr[1])) {
                shardingParam = new int[2];
                shardingParam[0] = Integer.valueOf(shardingArr[0]);
                shardingParam[1] = Integer.valueOf(shardingArr[1]);
            }
        }
        // 如果路由策略为分片广播，并且有分片参数，说明执行器组中的所有执行器都要执行任务，可以遍历调度所有执行器。
        if (ExecutorRouterStrategyEnum.SHARDING_BROADCAST == ExecutorRouterStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null)
                && !CollectionUtil.isEmpty(jobGroup.getRegistryList())
                && shardingParam == null) {
            // 遍历调度所有执行器
            for (int i = 0; i < jobGroup.getRegistryList().size(); i++) {
                processTrigger(jobGroup, jobInfo, finalFailRetryCount, triggerType, i, jobGroup.getRegistryList().size());
            }
        } else {
            // 如果路由策略不是分片广播，说明只有一个执行器要执行
            if (shardingParam == null) {
                shardingParam = new int[]{0, 1};
            }
            processTrigger(jobGroup, jobInfo, finalFailRetryCount, triggerType, shardingParam[0], shardingParam[1]);
        }
    }

    private static void processTrigger(XxlJobGroup group, XxlJobInfo jobInfo, int finalFailRetryCount, TriggerTypeEnum triggerType, int index, int total) {
        // 得到此任务的阻塞策略、路由策略
        ExecutorBlockStrategyEnum blockStrategy = ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), null);
        ExecutorRouterStrategyEnum routerStrategy = ExecutorRouterStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null);
        // 如果路由策略是分片广播，将分片参数拼接起来
        String shardingParam = routerStrategy == ExecutorRouterStrategyEnum.SHARDING_BROADCAST ? index + "/" + total : null;

        // 封装 日志 XxlJobLog，并将其保存在数据库中，保存后即可获得日志id
        XxlJobLog xxlJobLog = createXxlJobLog(jobInfo);
        XxlJobAdminConfig.getAdminConfig().getXxlJobLogMapper().save(xxlJobLog);
        logger.debug(">>>>>>>>>>> xxl-job trigger start, jobId:{}", jobInfo.getId());
        // 封装 触发参数 TriggerParam
        TriggerParam triggerParam = createTriggerParam(jobInfo, xxlJobLog, index, total);
        // 开始选取执行器。根据路由策略选择，如果是分片，就根据index选择。
        // 如果是其他策略，就根据具体的路由策略选择。
        String address = null;
        Result<String> routeAddressResult = null;
        List<String> registryList = group.getRegistryList();

        if (CollectionUtil.isEmpty(registryList)) {
            // 如果这个任务根本没有执行器
            routeAddressResult = Result.error(I18nUtil.getString("jobconf_trigger_address_empty"));
        } else {
            if (routerStrategy == ExecutorRouterStrategyEnum.SHARDING_BROADCAST) {
                // 如果路由策略为分片，为了防止index超过值，如果超过了，兜底一下，使用第一个执行器address
                address = index < registryList.size() ? registryList.get(index) : registryList.get(0);
            } else {
                routeAddressResult = routerStrategy.getExecutorRouter().route(triggerParam, registryList);
                address = routeAddressResult.getCode() == Result.SUCCESS_CODE ? routeAddressResult.getContent() : null;
            }
        }
        // 设置调度结果, 如果地址不为空，就去调度。如果为空，不需要调度
        Result<String> triggerResult = null;
        if (StringUtil.hasText(address)) {
            triggerResult = runExecutor(triggerParam, address);
        } else {
            triggerResult = Result.error(null);
        }
        // 调度执行完之后，拼接调度结果，完善XxlJobLog
        StringBuffer triggerMsgSb = new StringBuffer();
        triggerMsgSb.append(I18nUtil.getString("jobconf_trigger_type")).append("：").append(triggerType.getTitle());
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobconf_trigger_admin_adress")).append("：").append(IPUtil.getIp());
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobconf_trigger_exe_regtype")).append("：")
                .append((group.getAddressType() == 0) ? I18nUtil.getString("jobgroup_field_addressType_0") : I18nUtil.getString("jobgroup_field_addressType_1"));
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobconf_trigger_exe_regaddress")).append("：").append(group.getRegistryList());
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_executorRouteStrategy")).append("：").append(routerStrategy.getTitle());
        if (shardingParam != null) {
            triggerMsgSb.append("(" + shardingParam + ")");
        }
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_executorBlockStrategy")).append("：").append(blockStrategy.getTitle());
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_timeout")).append("：").append(jobInfo.getExecutorTimeout());
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_executorFailRetryCount")).append("：").append(finalFailRetryCount);
        triggerMsgSb.append("<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>" + I18nUtil.getString("jobconf_trigger_run") + "<<<<<<<<<<< </span><br>")
                .append((routeAddressResult != null && routeAddressResult.getMessage() != null) ? routeAddressResult.getMessage() + "<br><br>" : "").append(triggerResult.getMessage() != null ? triggerResult.getMessage() : "");

        xxlJobLog.setExecutorAddress(address);
        xxlJobLog.setExecutorHandler(jobInfo.getExecutorHandler());
        xxlJobLog.setExecutorParam(jobInfo.getExecutorParam());
        xxlJobLog.setExecutorShardingParam(shardingParam);
        xxlJobLog.setTriggerCode(triggerResult.getCode());
        xxlJobLog.setTriggerMsg(triggerMsgSb.toString());
        // 更新数据库中的调度的log (当然，执行完还会更新执行的log)
        XxlJobAdminConfig.getAdminConfig().getXxlJobLogMapper().updateTriggerInfo(xxlJobLog);
        logger.debug(">>>>>>>>>>>> xxl-job trigger end, jobId:{}", jobInfo.getId());
    }

    private static Result<String> runExecutor(TriggerParam triggerParam, String address) {
        Result<String> runResult = null;
        try {
            ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(address);
            runResult = executorBiz.run(triggerParam);
        } catch (Exception e) {
            logger.error(">>>>>>>>>> xxl-job trigger error, please check if the executor[{}] is running.", address, e);
            runResult = Result.error(e.getMessage());
        }
        String resultMessage = I18nUtil.getString("jobconf_trigger_run")  + "：" +
                "<br>address：" + address +
                "<br>code：" + runResult.getCode() +
                "<br>msg：" + runResult.getMessage();
        runResult.setMessage(resultMessage);
        return runResult;
    }


    private static boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private static TriggerParam createTriggerParam(XxlJobInfo jobInfo, XxlJobLog xxlJobLog, int index, int total) {
        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(jobInfo.getId());
        triggerParam.setExecutorHandler(jobInfo.getExecutorHandler());
        triggerParam.setExecutorParams(jobInfo.getExecutorParam());
        triggerParam.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
        triggerParam.setExecutorTimeout(jobInfo.getExecutorTimeout());
        triggerParam.setLogId(xxlJobLog.getId());
        triggerParam.setLogDateTime(xxlJobLog.getTriggerTime().getTime());
        triggerParam.setGlueType(jobInfo.getGlueType());
        triggerParam.setGlueType(jobInfo.getGlueSource());
        triggerParam.setGlueUpdatetime(jobInfo.getGlueUpdatetime().getTime());
        triggerParam.setBroadcastIndex(index);
        triggerParam.setBroadcastTotal(total);
        return triggerParam;
    }

    private static XxlJobLog createXxlJobLog(XxlJobInfo jobInfo) {
        XxlJobLog xxlJobLog = new XxlJobLog();
        xxlJobLog.setJobGroup(jobInfo.getJobGroup());
        xxlJobLog.setJobId(jobInfo.getId());
        xxlJobLog.setTriggerTime(new Date());
        return xxlJobLog;
    }
}
