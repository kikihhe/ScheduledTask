package com.xiaohe.core.thread;

import com.xiaohe.core.model.ScheduledTaskLogReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;

/**
 * @author : 小何
 * @Description : 日志展示的类
 * 1. 将日志展示给前端
 * 2. 清除过期日志
 * @date : 2023-09-17 15:20
 */
public class JobLogReportHelper {
    private static Logger logger = LoggerFactory.getLogger(JobLogReportHelper.class);

    private static JobRegistryHelper instance = new JobRegistryHelper();

    public static JobRegistryHelper getInstance() {
        return instance;
    }

    /**
     * 工作线程
     *
     */
    private Thread logThread;

    /**
     * 线程是否停止工作
     */
    private volatile boolean toStop = false;


    private void start() {
        logThread = new Thread(() -> {
            // 上一次清理日志的时间
            long lastCleanLogTime = 0;
            while (!toStop) {
                // 遍历三天的日志信息，今天、昨天、前天
                for (int i = 0; i < 3; i++) {
                    Calendar itemDay = Calendar.getInstance();
                    itemDay.add(Calendar.DAY_OF_MONTH, -i);
                    itemDay.set(Calendar.HOUR_OF_DAY, 0);
                    itemDay.set(Calendar.MINUTE, 0);
                    itemDay.set(Calendar.SECOND, 0);
                    itemDay.set(Calendar.MILLISECOND, 0);
                    // 这一天的开始时间
                    Date todayFrom = itemDay.getTime();
                    itemDay.set(Calendar.HOUR_OF_DAY, 23);
                    itemDay.set(Calendar.MINUTE, 59);
                    itemDay.set(Calendar.SECOND, 59);
                    itemDay.set(Calendar.MILLISECOND, 999);
                    // 这一天的结束时间
                    Date todayTo = itemDay.getTime();
                    // 创建一个report
                    ScheduledTaskLogReport logReport = new ScheduledTaskLogReport();
                    logReport.setTriggerDay(todayFrom);
                    logReport.setRunningCount(0);
                    logReport.setSucCount(0);
                    logReport.setFailCount(0);




                }

            }
        });
    }


    /**
     * 停止该组件
     */
    private void toStop() {
        toStop = true;
        logThread.interrupt();
        try {
            logThread.join();
        } catch (InterruptedException e) {
          logger.error(e.getMessage(), e);
        }
    }
}
