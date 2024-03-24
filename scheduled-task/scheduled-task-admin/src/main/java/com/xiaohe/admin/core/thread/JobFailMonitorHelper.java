package com.xiaohe.admin.core.thread;

import com.xiaohe.admin.core.conf.ScheduleTaskAdminConfig;
import com.xiaohe.admin.core.model.XxlJobInfo;
import com.xiaohe.admin.core.model.XxlJobLog;
import com.xiaohe.admin.core.trigger.TriggerTypeEnum;
import com.xiaohe.admin.core.util.I18nUtil;
import com.xiaohe.core.util.CollectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author : 小何
 * @Description :任务失败，发送告警的
 * @date : 2023-10-07 23:19
 */
public class JobFailMonitorHelper {
    private static Logger logger = LoggerFactory.getLogger(JobFailMonitorHelper.class);

    private static JobFailMonitorHelper instance = new JobFailMonitorHelper();

    public static JobFailMonitorHelper getInstance() {
        return instance;
    }

    /**
     * 处理失败任务告警的线程
     */
    private Thread monitorThread;

    /**
     * 该组件是否结束
     */
    private volatile boolean toStop = false;


    public void start() {
        monitorThread = new Thread(() -> {
            while (!toStop) {
                try {
                    // 从数据库中查询失败的任务, 一次1000个
                    List<XxlJobLog> failJobLogs = ScheduleTaskAdminConfig.getAdminConfig().getXxlJobLogMapper().findFailJobLogs(1000);
                    if (!CollectionUtil.isEmpty(failJobLogs)) {
                        alarm(failJobLogs);
                    }
                } catch (Exception e) {
                    if (!toStop) {
                        logger.warn(e.getMessage(), e);
                    }
                }
                // 10s一次
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException e) {
                    if (!toStop) {
                        logger.warn(e.getMessage(), e);
                    }
                }
            }
            logger.info(">>>>>>>>>>> Scheduled Task, job fail monitor thread stop");
        });
        monitorThread.setDaemon(true);
        monitorThread.setName("Scheduled Task, admin JobFailMonitorHelper");
        monitorThread.start();

        logger.info(">>>>>>>>>>>> Scheduled Task, JobFailMonitorHelper start success");
    }

    /**
     * 开始报警
     *
     * @param failJobLogs
     */
    private void alarm(List<XxlJobLog> failJobLogs) {
        for (XxlJobLog failJobLog : failJobLogs) {
            // 先将报警状态改为 锁定， 即 alarm_status = -1.
            // 告警状态：0:默认、-1:锁定状态、1:无需告警、2:告警成功、3:告警失败
            // 为什么这样做，CAS操作 防止调度中心集群时重复告警
            int lockRet = ScheduleTaskAdminConfig.getAdminConfig().getXxlJobLogMapper().updateAlarmStatusInt(failJobLog.getId(), 0, -1);
            // 方法外部已经判断过肯定存在还未告警的任务，但此时修改状态却失败. 说明修改前有其他调度中心已经将这个失败任务告警了，那就直接下一个
            if (lockRet < 1) {
                continue;
            }
            // 再查一次
            failJobLog = ScheduleTaskAdminConfig.getAdminConfig().getXxlJobLogMapper().load(failJobLog.getId());
            XxlJobInfo info = ScheduleTaskAdminConfig.getAdminConfig().getXxlJobInfoMapper().loadById(failJobLog.getJobId());
            // 如果这个失败的任务还有重试机会，那就继续调用
            if (failJobLog.getExecutorFailRetryCount() > 0) {
                JobTriggerPoolHelper.trigger(failJobLog.getJobId(), TriggerTypeEnum.RETRY, (failJobLog.getExecutorFailRetryCount()-1), failJobLog.getExecutorShardingParam(), failJobLog.getExecutorParam(), null);
                String retryMsg = "<br><br><span style=\"color:#F39C12;\" > >>>>>>>>>>>"+ I18nUtil.getString("jobconf_trigger_type_retry") +"<<<<<<<<<<< </span><br>";
                failJobLog.setTriggerMsg(failJobLog.getTriggerMsg() + retryMsg);
                //跟新数据库的信息，就是把XxlJobLog更新一下，因为这个定时任务的日志中记录了失败重试的信息
                ScheduleTaskAdminConfig.getAdminConfig().getXxlJobLogMapper().updateTriggerInfo(failJobLog);
            }
            int newAlarmStatus = 0;
            if (info != null) {
                boolean alarm = ScheduleTaskAdminConfig.getAdminConfig().getJobAlarmer().alarm(info, failJobLog);
                newAlarmStatus = alarm ? 2 : 3;
            } else {
                newAlarmStatus = 1;
            }
            // 更新数据库状态
            ScheduleTaskAdminConfig.getAdminConfig().getXxlJobLogMapper().updateAlarmStatusInt(failJobLog.getId(), -1, newAlarmStatus);
        }


    }

    /**
     * 停止该组件
     */
    public void toStop() {
        toStop = true;
        monitorThread.interrupt();
        try {
            monitorThread.join();
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }
    }

}
