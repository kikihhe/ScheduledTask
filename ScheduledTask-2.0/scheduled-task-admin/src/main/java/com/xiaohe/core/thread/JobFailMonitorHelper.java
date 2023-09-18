package com.xiaohe.core.thread;

import com.xiaohe.context.ScheduledTaskContext;
import com.xiaohe.core.conf.ScheduledTaskAdminConfig;
import com.xiaohe.core.model.ScheduledTaskInfo;
import com.xiaohe.core.model.ScheduledTaskLog;
import com.xiaohe.core.trigger.TriggerTypeEnum;
import com.xiaohe.mapper.ScheduledTaskLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author : 小何
 * @Description : 失败任务重试功能
 * @date : 2023-09-17 16:35
 */
public class JobFailMonitorHelper {
    private static Logger logger = LoggerFactory.getLogger(JobFailMonitorHelper.class);

    private static JobFailMonitorHelper instance = new JobFailMonitorHelper();

    public static JobFailMonitorHelper getInstance() {
        return instance;
    }

    /**
     * 重试失败任务的线程
     */
    private Thread monitorThread;

    /**
     * 失败重试线程是否停止工作
     */
    private volatile boolean toStop;

    public void start() {
        monitorThread = new Thread(() -> {
            while (!toStop) {
                // 从数据库中查找失败的任务，每次最多1000
                List<Long> failLogIds =
                        ScheduledTaskAdminConfig.getAdminConfig().getScheduledTaskLogMapper().findFailJobLogIds(1000);
                for (Long failLogId : failLogIds) {
                    // 告警状态: -1: 锁定状态, 0:未告警 1：无需告警 2: 告警成功 3:告警失败
                    // 将这个任务的告警状态改为-1，目的是防止调度中心集群时并发更改，优点乐观锁那味儿了
                    int update = ScheduledTaskAdminConfig.getAdminConfig()
                            .getScheduledTaskLogMapper().updateAlarmStatus(failLogId, 0, -1);
                    if (update < 1) {
                        continue;
                    }
                    ScheduledTaskLog log = ScheduledTaskAdminConfig.getAdminConfig()
                            .getScheduledTaskLogMapper().load(failLogId);
                    // 先进行重试
                    ScheduledTaskInfo job = ScheduledTaskAdminConfig.getAdminConfig()
                            .getScheduledTaskInfoMapper().loadById(log.getJobId());
                    if (job.getExecutorFailRetryCount() > 0) {
                        // 调度
                        JobTriggerPoolHelper.trigger(job.getId(), TriggerTypeEnum.RETRY, job.getExecutorFailRetryCount() - 1, job.getExecutorShardingParam(), job.getExecutorParam(), null);
                        // 给日志加上失败重试的信息
                        String retryMsg = "<br><br><span style=\"color:#F39C12;\" > >>>>>>>>>>>"+ "jobconf_trigger_type_retry" +"<<<<<<<<<<< </span><br>";
                        log.setTriggerMsg(log.getTriggerMsg() + retryMsg);
                        ScheduledTaskAdminConfig.getAdminConfig().getScheduledTaskLogMapper().updateTriggerInfo(log);
                    }
                    // 再进行报警
                    int newAlarmStatus = -1;
                    if (job != null) {
                        boolean alarmResult = ScheduledTaskAdminConfig.getAdminConfig()
                                .getJobAlarmer().alarm(job, log);
                        newAlarmStatus = alarmResult ? 2 : 3;
                    }
                    ScheduledTaskAdminConfig.getAdminConfig().getScheduledTaskLogMapper().updateAlarmStatus(failLogId, -1, newAlarmStatus);
                }
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }

            }

        });
        monitorThread.setDaemon(true);
        monitorThread.setName("xxl-job, admin JobFailMonitorHelper");
        monitorThread.start();
    }

    public void toStop() {
        toStop = true;
        monitorThread.interrupt();
        try {
            monitorThread.join();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
