package com.xiaohe.thread;

import com.xiaohe.log.ScheduledTaskFileAppender;
import com.xiaohe.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author : 小何
 * @Description : 定期清除过期日志
 * @date : 2023-09-19 19:47
 */
public class JobLogFileCleanThread {
    private static Logger logger = LoggerFactory.getLogger(JobLogFileCleanThread.class);

    private static JobLogFileCleanThread instance = new JobLogFileCleanThread();

    public static JobLogFileCleanThread getInstance() {
        return instance;
    }

    /**
     * 工作线程
     */
    private Thread localThread;

    /**
     * 线程停止标识
     */
    private volatile boolean toStop = false;

    public void start(final long logRetentionDays) {
        localThread = new Thread(() -> {
            while (!toStop) {
                // 获取今天的时间
                long today = new Date().getTime();
                // 获取指定路径下的所有文件夹，这些文件夹都以日期命名，如 2023-01-01
                // 而在这些文件夹里面才是以定时任务的id命名的日志文件
                File[] logFiles = new File(ScheduledTaskFileAppender.getLogPath()).listFiles();
                for (File logFile : logFiles) {
                    // 如果不是文件夹就跳过, 如果文件名中不包含 - 就跳过
                    if (!logFile.isDirectory() || !logFile.getName().contains("-")) {
                        continue;
                    }
                    Date dirCreateTime = null;
                    try {
                        dirCreateTime = new SimpleDateFormat("yyyy-MM-dd").parse(logFile.getName());
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                    if (dirCreateTime == null) {
                        continue;
                    }
                    if (today - dirCreateTime.getTime() > logRetentionDays* (24*60*60*1000)) {
                        FileUtil.deleteRecursively(logFile);
                    }
                }
            }
        });
        localThread.setDaemon(true);
        localThread.setName("xxl-job, executor JobLogFileCleanThread");
        localThread.start();
    }

    /**
     * 停止过期日志的清除
     */
    public void toStop() {
        toStop = true;
        localThread.interrupt();
        try {
            localThread.join();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
