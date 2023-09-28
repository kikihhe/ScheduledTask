package com.xiaohe.core.thread;

import com.xiaohe.core.log.XxlJobFileAppender;
import com.xiaohe.core.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author : 小何
 * @Description : 清除过期日志
 * @date : 2023-09-28 21:11
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
     * 该组件是否停止工作
     */
    private volatile boolean toStop = false;

    public void start(final long logRetentionDays) {
        if (logRetentionDays < 1) {
            return;
        }
        localThread = new Thread(() -> {
           while (!toStop) {
               // 获取日志文件夹下的所有以日期命名的文件夹
               File[] childDirs = new File(XxlJobFileAppender.getLogBasePath()).listFiles();
               // 获取今天零点时间
               Date today = getToday();
               for (File childFile : childDirs) {
                   // 找的是文件夹
                   if (!childFile.isDirectory()) {
                       continue;
                   }
                   // 如果文件夹不符合格式 (2021-01-01)，直接下一位
                   Date date = checkFileName(childFile.getName());
                   if (date == null) continue;
                   // 如果今天的时间 - 该文件创建的时间 >  logRetentionDays，就将此文件夹删除
                   if (today.getTime() - date.getTime() >= logRetentionDays*(24 * 60 * 60 * 1000)) {
                       FileUtil.deleteRecursively(childFile);
                   }
               }
               // 一天一次
               try {
                   TimeUnit.DAYS.sleep(1);
               } catch (InterruptedException e) {
                   if (!toStop) {
                       logger.error(e.getMessage());
                   }
               }
           }
            logger.info(">>>>>>>>>>> xxl-job, executor JobLogFileCleanThread thread destroy.");
        });
        localThread.setDaemon(true);
        localThread.setName("xxl-job, executor JobLogFileCleanThread");
        localThread.start();
    }

    /**
     * 检查文件名是否符合格式
     * @param fileName
     * @return
     */
    private Date checkFileName(String fileName) {
        Date logFileCreateDate = null;
        try {
            logFileCreateDate = (new SimpleDateFormat("yyyy-MM-dd")).parse(fileName);
        } catch (ParseException e) {
            logger.error(e.getMessage());
        }
        return logFileCreateDate;
    }

    /**
     * 得到今天零点时间
     * @return
     */
    private Date getToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    /**
     * 终止该组件运行
     */
    public void toStop() {
        toStop = true;
        if (localThread == null) {
            return;
        }
        localThread.interrupt();
        try {
            localThread.join();
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }

    }
}
