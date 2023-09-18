package com.xiaohe.core.trigger;

import com.xiaohe.biz.ExecutorBiz;
import com.xiaohe.biz.model.Result;
import com.xiaohe.biz.model.TriggerParam;
import com.xiaohe.core.conf.ScheduledTaskAdminConfig;
import com.xiaohe.core.model.ScheduledTaskGroup;
import com.xiaohe.core.model.ScheduledTaskInfo;
import com.xiaohe.core.model.ScheduledTaskLog;
import com.xiaohe.core.route.ExecutorRouteStrategyEnum;
import com.xiaohe.core.scheduler.TaskScheduler;
import com.xiaohe.core.util.I18nUtil;
import com.xiaohe.util.IpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

/**
 * @author : 小何
 * @Description : 调度中心负责发送调度任务给执行器的类
 * @date : 2023-09-01 12:42
 */
public class ScheduledTaskTrigger {

    private static Logger logger = LoggerFactory.getLogger(ScheduledTaskTrigger.class);


    public static void trigger(int jobId,
                               TriggerTypeEnum triggerType,
                               int failRetryCount,
                               String executorShardingParam,
                               String executorParam,
                               String addressList) {
        ScheduledTaskInfo jobInfo = ScheduledTaskAdminConfig.getAdminConfig().getScheduledTaskInfoMapper().loadById(jobId);
        if (jobInfo == null) {
            logger.error(">>>>>>>>>>>> trigger fail, jobId invalid，jobId={}", jobId);
            return;
        }
        // 用户在web界面指定参数了就设置参数，然后从数据库中查出有这个任务的执行器组，
        if (!executorParam.isEmpty()) {
            jobInfo.setExecutorParam(executorParam);
        }
        ScheduledTaskGroup group = ScheduledTaskAdminConfig.getAdminConfig().getScheduledTaskGroupMapper().loadById(jobInfo.getJobGroup());
        // 如果用户在web界面制定了执行器IP，那就使用用户指定的
        // 如果为空，可能是失败任务重试，让它使用原先已经注册好的
        if (addressList != null && !addressList.trim().isEmpty()) {
            // 执行器的注册方式改为手动注册
            group.setAddressType(1);
            group.setAddressList(addressList.trim());
        }
        processTrigger(group, jobInfo, -1, triggerType, 0, 1);
    }

    /**
     * 进一步处理任务的调度，封装日志、路由策略、分片策略
     *
     * @param group               部署了该定时任务的执行器组
     * @param jobInfo             定时任务信息
     * @param finalFailRetryCount 失败重试次数
     * @param triggerType         调度类型，基本都是cron
     * @param index               分片索引
     * @param total               分片总数
     */
    private static void processTrigger(ScheduledTaskGroup group,
                                       ScheduledTaskInfo jobInfo,
                                       int finalFailRetryCount,
                                       TriggerTypeEnum triggerType,
                                       int index,
                                       int total) {
        // 得到调度的路由策略
        ExecutorRouteStrategyEnum executorRouteStrategyEnum = ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null);
        // 创建日志对象，开始记录任务执行过程中的信息
        ScheduledTaskLog jobLog = new ScheduledTaskLog();
        jobLog.setJobGroup(jobInfo.getJobGroup());
        jobLog.setJobId(jobInfo.getId());
        jobLog.setTriggerTime(new Date());
        // 将日志保存到数据库，保存后就能得到日志的id
        ScheduledTaskAdminConfig.getAdminConfig().getScheduledTaskLogMapper().save(jobLog);
        logger.debug(">>>>>>>>>>> scheduled_task trigger start, jobId:{}", jobLog.getId());

        // 初始化调度参数
        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(jobInfo.getId());
        triggerParam.setExecutorHandler(jobInfo.getExecutorHandler());
        triggerParam.setExecutorParams(jobInfo.getExecutorParam());
        triggerParam.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
        triggerParam.setLogId(jobLog.getId());
        triggerParam.setLogDateTime(jobLog.getTriggerTime().getTime());
        triggerParam.setGlueType(jobInfo.getGlueType());

        // 通过路由策略选择一个执行器去执行
        String address = null;
        Result<String> routeAddressResult = null;
        List<String> registryList = group.getRegistryList();
        if (registryList != null && !registryList.isEmpty()) {
            routeAddressResult = executorRouteStrategyEnum.getRouter().route(triggerParam, registryList);
            if (routeAddressResult.getCode() == Result.SUCCESS_CODE) {
                address = routeAddressResult.getContent();
            } else {
                // 如果没得到地址，说明路由失败，先记录结果
                routeAddressResult = new Result<>(Result.FAIL_CODE, "jobconf_trigger_address_empty");
            }
        }

        Result<String> triggerResult = null;
        // 如果不为空，可以进行远程调用，如果为空，记录信息
        if (address != null && !address.trim().isEmpty()) {
            triggerResult = runExecutor(triggerParam, address);
        } else {
            triggerResult = new Result<String>(Result.FAIL_CODE, null);
        }

        //在这里拼接一下触发任务的信息，其实就是web界面的调度备注
        StringBuffer triggerMsgSb = new StringBuffer();
        triggerMsgSb.append("jobconf_trigger_type").append("：").append(triggerType.getTitle());
        triggerMsgSb.append("<br>").append("jobconf_trigger_admin_adress").append("：").append(IpUtil.getIp());
        triggerMsgSb.append("<br>").append("jobconf_trigger_exe_regtype").append("：")
                .append((group.getAddressType() == 0) ? "jobgroup_field_addressType_0" : "jobgroup_field_addressType_1");
        triggerMsgSb.append("<br>").append("jobconf_trigger_exe_regaddress").append("：").append(group.getRegistryList());
        triggerMsgSb.append("<br>").append("jobinfo_field_executorRouteStrategy").append("：").append(executorRouteStrategyEnum.getTitle());

        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_timeout")).append("：").append(jobInfo.getExecutorTimeout());
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_executorFailRetryCount")).append("：").append(finalFailRetryCount);
        triggerMsgSb.append("<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>" + I18nUtil.getString("jobconf_trigger_run") + "<<<<<<<<<<< </span><br>")
                .append((routeAddressResult != null && routeAddressResult.getMessage() != null) ? routeAddressResult.getMessage() + "<br><br>" : "").append(triggerResult.getMessage() != null ? triggerResult.getMessage() : "");

        // 继续更新日志信息
        jobLog.setExecutorAddress(address);
        jobLog.setExecutorHandler(jobInfo.getExecutorHandler());
        jobLog.setExecutorParam(jobInfo.getExecutorParam());
        jobLog.setExecutorFailRetryCount(finalFailRetryCount);
        jobLog.setTriggerCode(triggerResult.getCode());
        jobLog.setTriggerMsg(triggerMsgSb.toString());
        ScheduledTaskAdminConfig.getAdminConfig().getScheduledTaskLogMapper().updateTriggerInfo(jobLog);

    }

    /**
     * 进行远程调用
     * @param triggerParam
     * @param address
     * @return
     */
    private static Result<String> runExecutor(TriggerParam triggerParam, String address) {
        Result<String> runResult = null;
        try {
            ExecutorBiz executorBiz = TaskScheduler.getExecutorBiz(address);
            runResult = executorBiz.run(triggerParam);
        } catch (Exception e) {
            logger.error(">>>>>>>>>>> scheduled_task trigger error, please check if the executor[{}] is running.", address, e);
            runResult = new Result<String>(Result.FAIL_CODE, e.toString());
        }

        //在这里拼接一下远程调用返回的状态码和消息
        StringBuffer runResultSB = new StringBuffer(I18nUtil.getString("jobconf_trigger_run") + "：");
        runResultSB.append("<br>address：").append(address);
        runResultSB.append("<br>code：").append(runResult.getCode());
        runResultSB.append("<br>msg：").append(runResult.getMessage());
        runResult.setMessage(runResultSB.toString());
        return runResult;
    }


}
