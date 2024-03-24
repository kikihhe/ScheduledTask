package com.xiaohe.admin.core.thread;


import com.xiaohe.admin.core.conf.ScheduleTaskAdminConfig;
import com.xiaohe.admin.core.model.XxlJobLogReport;
import com.xiaohe.core.util.CollectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author : 小何
 * @Description : <br/>
 * 1. 从 xxl_job_log 中统计今天、昨天、前天 执行任务的数量，成功/失败/正在执行的数量 到 xxl_job_log_report 中   <br/>
 * 2. 从 xxl_job_log 中删除过期的日志，只要超过了配置文件中配置的日志，都需要删除
 * @date : 2023-10-15 17:34
 */
public class JobLogReportHelper {
    private static Logger logger = LoggerFactory.getLogger(JobLogReportHelper.class);

    private static JobLogReportHelper instance = new JobLogReportHelper();

    public static JobLogReportHelper getInstance() {
        return instance;
    }


    /**
     * 工作线程
     */
    private Thread logThread;

    /**
     * 组件是否结束
     */
    private volatile boolean toStop = false;

    public void start() {
        logThread = new Thread(() -> {
            long lastCleanLogTime = 0;
            while (!toStop) {
                // 统计日志，统计今天、昨天、前天 执行任务数量、任务成功数、任务失败数
                analysisLog();
                // 删除过期日志
                cleanExpiredLogs(lastCleanLogTime);
                lastCleanLogTime = System.currentTimeMillis();

                // 一分钟一次
                try {
                    TimeUnit.MINUTES.sleep(1);
                } catch (InterruptedException e) {
                    if (!toStop) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
            logger.info(">>>>>>>>>>> Scheduled Task, job log report thread stop");
        });
        logThread.setDaemon(true);
        logThread.setName("Scheduled Task, admin JobLogReportHelper");
        logThread.start();

        logger.info(">>>>>>>>>>>> Scheduled Task, JobLogReportHelper start success");
    }

    /**
     * 统计日志，统计今天、昨天、前天 执行任务数量、任务成功数、任务失败数
     */
    public void analysisLog() {
        // 遍历今天 昨天 前天
        for (int i = 0; i < 3; i++) {
            Date toDayBegin = getTodayBegin(-i);
            Date todayEnd = getTodayEnd(-i);
            // 统计这一天的任务
            XxlJobLogReport xxlJobLogReport = new XxlJobLogReport();
            xxlJobLogReport.setTriggerDay(toDayBegin);
            xxlJobLogReport.setRunningCount(0);
            xxlJobLogReport.setSucCount(0);
            xxlJobLogReport.setFailCount(0);
            // 从数据库中查出一个Map，有三个key
            // triggerDayCount: 这一天调度了多少个任务
            // triggerDayCountRunning: 查的时候有多少任务正在运行。其实并不准确，查的时候查的是 trigger_code=200 && handle_code = 0 的任务
            // triggerDayCountSuc: 这一天运行成功多少次任务
            Map<String, Object> triggerCountMap = ScheduleTaskAdminConfig.getAdminConfig().getXxlJobLogMapper().findLogReport(toDayBegin, todayEnd);
            if (!CollectionUtil.isEmpty(triggerCountMap)) {
                int triggerDayCount = triggerCountMap.containsKey("triggerDayCount") ? Integer.valueOf(String.valueOf(triggerCountMap.get("triggerDayCount"))) : 0;
                int triggerDayCountRunning = triggerCountMap.containsKey("triggerDayCountRunning") ? Integer.valueOf(String.valueOf(triggerCountMap.get("triggerDayCountRunning"))) : 0;
                int triggerDayCountSuc = triggerCountMap.containsKey("triggerDayCountSuc") ? Integer.valueOf(String.valueOf(triggerCountMap.get("triggerDayCountSuc"))) : 0;
                int triggerDayCountFail = triggerDayCount - triggerDayCountSuc;
                // 设置最新的消息
                xxlJobLogReport.setRunningCount(triggerDayCountRunning);
                xxlJobLogReport.setSucCount(triggerDayCountSuc);
                xxlJobLogReport.setFailCount(triggerDayCountFail);
            }
            int ret = ScheduleTaskAdminConfig.getAdminConfig().getXxlJobLogReportMapper().update(xxlJobLogReport);
            if (ret < 1) {
                ScheduleTaskAdminConfig.getAdminConfig().getXxlJobLogReportMapper().save(xxlJobLogReport);
            }

        }
    }

    /**
     * 根据配置文件，清除过期日志
     */
    public void cleanExpiredLogs(long lastCleanLogTime) {
        if (ScheduleTaskAdminConfig.getAdminConfig().getLogretentiondays() > 0
                && System.currentTimeMillis() >= lastCleanLogTime + 24 * 60 * 60 * 1000) {
            // 得到需要清除那天的时间，从数据中删除这个时间之前的所有日志
            Date expiredDay = getTodayBegin(-1 * ScheduleTaskAdminConfig.getAdminConfig().getLogretentiondays());
            List<Long> cleanLogIds = null;
            do {
                cleanLogIds = ScheduleTaskAdminConfig.getAdminConfig().getXxlJobLogMapper().findClearJobLog(0, 0, expiredDay, 0, 1000);
                if (!CollectionUtil.isEmpty(cleanLogIds)) {
                    ScheduleTaskAdminConfig.getAdminConfig().getXxlJobLogMapper().clearLogs(cleanLogIds);
                }

            } while (!CollectionUtil.isEmpty(cleanLogIds));
        }

    }

    /**
     * 停止组件
     */
    public void toStop() {
        toStop = true;
        logThread.interrupt();
        try {
            logThread.interrupt();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * 得到指定天数的零点零时零分零秒.. add : 代表今天加上几天
     *
     * @param add
     */
    private Date getTodayBegin(int add) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -add);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date getTodayEnd(int add) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -add);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }

}
