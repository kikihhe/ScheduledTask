package com.xiaohe.log;

import com.xiaohe.biz.model.LogResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author : 小何
 * @Description : 操作文件(日志)的类, 记录日志、保存glue代码
 * @date : 2023-09-02 12:22
 */
public class ScheduledTaskFileAppender {
    private static Logger logger = LoggerFactory.getLogger(ScheduledTaskFileAppender.class);

    /**
     * 默认日志存储路径，执行器启动时会被配置文件中的路径代替
     */
    private static String logBasePath = "/data/applogs/scheduled-task/jobhandler";

    /**
     * 这个路径用于存储web端编辑的代码
     */
    private static String glueSrcPath = logBasePath.concat("/gluesource");

    /**
     * 初始化存储 日志文件 路径的方法
     *
     * @param logPath
     */
    public static void initLogPath(String logPath) {
        // 创建日志文件夹
        if (logPath != null && !logPath.isEmpty()) {
            logBasePath = logPath;
        }
        File logPathDir = new File(logBasePath);
        if (!logPathDir.exists()) {
            logPathDir.mkdirs();
        }
        logBasePath = logPathDir.getPath();

        // 创建 glue代码文件夹
        File glueBaseDir = new File(logPathDir, "gluesource");
        if (!glueBaseDir.exists()) {
            glueBaseDir.mkdirs();
        }
        glueSrcPath = glueBaseDir.getPath();
    }

    public static String getLogPath() {
        return logBasePath;
    }

    public static String getGlueSrcPath() {
        return glueSrcPath;
    }

    /**
     * 根据 定时任务的触发时间 和 日志id 生成一个文件名      <br></br>
     * 先根据日期创建一个文件夹，不同的日志文件以 日期目录 分割      <br></br>
     * /2023-09-01/2.log                        <br></br>
     * /2023-09-01/3.log                        <br></br>
     * /2023-09-01/5.log                        <br></br>
     * <br></br>
     * /2023-09-02/1.log                        <br></br>
     */
    public static String makeLogFileName(Date triggerDate, long logId) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        // 创建一个路径为 logBasePath + “/” + triggerDate 的文件夹
        // 假如 日期为 2023/09/01，那么该文件夹的名称为: "/data/applogs/scheduled-task/jobhandler/2023-09-01"
        File logFilePath = new File(getLogPath(), sdf.format(triggerDate));
        if (!logFilePath.exists()) {
            logFilePath.mkdir();
        }
        // 假如logId = 2, 最后拼接的文件的文件名为: "/data/applogs/scheduled-task/jobhandler/2023-09-01/2.log"
        String logFileName = logFilePath.getPath()
                .concat(File.separator) // 文件系统默认分隔符, win = \， linux = /
                .concat(String.valueOf(logId))
                .concat(".log");
        return logFileName;
    }


    /**
     * 将appendLog写到文件名为 logFileName 的文件中
     *
     * @param logFileName 目标文件名
     * @param appendLog   写入内容
     */
    public static void appendLog(String logFileName, String appendLog) {
        if (logFileName == null || logFileName.trim().isEmpty()) {
            return;
        }
        File logFile = new File(logFileName);
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();

            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                return;
            }
        }
        if (appendLog == null) {
            appendLog = "";
        }
        appendLog += "\r\n";
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(logFile);
            fos.write(appendLog.getBytes(StandardCharsets.UTF_8));
            fos.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

    }


    /**
     * 从指定行开始读取日志文件内容
     *
     * @param logFileName 文件巨路径
     * @param fromLineNum 开始行数
     */
    public static LogResult readLog(String logFileName, int fromLineNum) {
        if (logFileName == null || logFileName.trim().isEmpty()) {
            return new LogResult(fromLineNum, 0, "readLog fail, logFile not found", true);
        }
        File logFile = new File(logFileName);
        if (!logFile.exists()) {
            return new LogResult(fromLineNum, 0, "readLog fail, logFile not found", true);
        }
        StringBuffer logContentBuffer = new StringBuffer();
        int toLineNum = 0;
        LineNumberReader reader = null;
        try {
            reader = new LineNumberReader(new InputStreamReader(Files.newInputStream(logFile.toPath()), StandardCharsets.UTF_8));
            String line = "";
            while ((line = reader.readLine()) != null) {
                toLineNum = reader.getLineNumber();
                if (toLineNum >= fromLineNum) {
                    logContentBuffer.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        LogResult logResult = new LogResult(fromLineNum, toLineNum, logContentBuffer.toString(), false);
        return logResult;
    }

    /**
     * 只读一行文件
     * @param logFile
     * @return
     */
    public static String readLines(File logFile){
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(Files.newInputStream(logFile.toPath()), StandardCharsets.UTF_8));
            if (reader != null) {
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        return null;
    }


}
