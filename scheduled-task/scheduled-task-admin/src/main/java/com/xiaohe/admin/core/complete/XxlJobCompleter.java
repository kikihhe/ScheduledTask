package com.xiaohe.admin.core.complete;

import com.xiaohe.admin.core.conf.ScheduleTaskAdminConfig;
import com.xiaohe.admin.core.model.XxlJobInfo;
import com.xiaohe.admin.core.model.XxlJobLog;
import com.xiaohe.admin.core.thread.JobTriggerPoolHelper;
import com.xiaohe.admin.core.trigger.TriggerTypeEnum;
import com.xiaohe.admin.core.util.I18nUtil;
import com.xiaohe.core.context.XxlJobContext;
import com.xiaohe.core.model.Result;
import com.xiaohe.core.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;

/**
 * @author : 小何
 * @Description : 执行子任务并将子任务的执行结果记录在 handleMsg 中。
 * @date : 2023-10-13 13:20
 */
public class XxlJobCompleter {
    private static Logger logger = LoggerFactory.getLogger(XxlJobCompleter.class);

    /**
     * 1. 调度子任务，等待执行结果并记录日志 (记录再在 XxlJobLog的HandleMessage后面)
     * 2. 如果这个任务太长了就切割一下，最长为15000字符
     *
     * @param xxlJobLog
     */
    public static int updateHandleInfoAndFinish(XxlJobLog xxlJobLog) {
        // 执行子任务
        finishJob(xxlJobLog);
        // 切割字符串
        if (xxlJobLog.getHandleMsg().length() > 15000) {
            xxlJobLog.setHandleMsg(xxlJobLog.getHandleMsg().substring(0, 15000));
        }
        // 更新任务Log信息（只更新执行信息）
        return ScheduleTaskAdminConfig.getAdminConfig().getXxlJobLogMapper().updateHandleInfo(xxlJobLog);
    }

    /**
     * 执行该任务的所有子任务
     *
     * @param xxlJobLog
     */
    public static void finishJob(XxlJobLog xxlJobLog) {
        String triggerChildMsg = null;
        // 如果任务执行失败，就不需要再执行子任务了。如果没有子任务，更不需要执行
        if (xxlJobLog == null || xxlJobLog.getHandlerCode() != XxlJobContext.HANDLE_CODE_SUCCESS) {
            return;
        }
        XxlJobInfo xxlJobInfo = ScheduleTaskAdminConfig.getAdminConfig().getXxlJobInfoMapper().loadById(xxlJobLog.getJobId());
        if (xxlJobInfo == null || !StringUtil.hasText(xxlJobInfo.getChildJobId())) {
            return;
        }
        // 有子任务，开始执行
        triggerChildMsg = "<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>" + I18nUtil.getString("jobconf_trigger_child_run") + "<<<<<<<<<<< </span><br>";
        String[] childJobIds = xxlJobInfo.getChildJobId().split(",");
        for (int i = 0; i < childJobIds.length; i++) {
            int childJobId = isNumber(childJobIds[i]) ? Integer.parseInt(childJobIds[i]) : -1;
            if (childJobId == -1) {
                triggerChildMsg += MessageFormat.format(I18nUtil.getString("jobconf_callback_child_msg2"), (i + 1), childJobIds.length, childJobIds[i]);
                continue;
            }
            JobTriggerPoolHelper.trigger(childJobId, TriggerTypeEnum.PARENT, -1, null, null, null);
            Result<String> triggerChildResult = Result.SUCCESS;
            triggerChildMsg += MessageFormat.format(I18nUtil.getString("jobconf_callback_child_msg1"),
                    (i + 1),
                    childJobIds.length,
                    childJobIds[i],
                    (triggerChildResult.getCode() == Result.SUCCESS_CODE ? I18nUtil.getString("system_success") : I18nUtil.getString("system_fail")),
                    triggerChildResult.getMessage());
        }
        if (StringUtil.hasText(triggerChildMsg)) {
            xxlJobLog.setHandleMsg(xxlJobLog.getHandleMsg() + triggerChildMsg);
        }
    }

    public static boolean isNumber(String str) {
        try {
            int a = Integer.parseInt(str);
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
