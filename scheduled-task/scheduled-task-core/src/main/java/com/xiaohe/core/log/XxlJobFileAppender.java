package com.xiaohe.core.log;

import com.xiaohe.core.model.LogResult;
import com.xiaohe.core.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author : 小何
 * @Description : 日志操作类
 * @date : 2023-09-28 20:04
 */
public class XxlJobFileAppender {
    private static Logger logger = LoggerFactory.getLogger(XxlJobFileAppender.class);

    /**
     * 默认的日志存储路径，可以在配置文件中更改
     */
    private static String logBasePath = "/data/applogs/xxl-job/jobhandler";

    /**
     * 存储在线编辑的代码
     */
    private static String glueSrcPath = logBasePath.concat("/gluesource");

    /**
     * 初始化日志文件路径，创建日志文件夹
     *
     * @param logPath 配置文件中指定的日志路径
     */
    public static void initLogPath(String logPath) {
        // 如果配置文件中指定了就使用指定的
        if (StringUtil.hasText(logPath)) {
            logBasePath = logPath;
        }
        // 文件夹不存在就新建
        File logPathDir = new File(logBasePath);
        if (!logPathDir.exists()) {
            logPathDir.mkdirs();
        }
        logBasePath = logPathDir.getPath();
        File glueBaseDir = new File(logPathDir, "gluesource");
        if (!glueBaseDir.exists()) {
            glueBaseDir.mkdirs();
        }
        glueSrcPath = glueBaseDir.getPath();
    }

    /**
     * 根据任务的触发时间和任务id创建日志文件，触发时间为文件夹路径，任务id为日志文件名      <br></br>
     * 如 /2023-03-01/1.log
     * (如果日志文件夹不存在就创建，但不创建日志文件，只获得名称)
     *
     * @param triggerDate
     * @param logId
     */
    public static String makeLogFileName(Date triggerDate, long logId) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        // 先根据baseLog路径 + 日期 创建文件夹
        File logFilePath = new File(logBasePath, sdf.format(triggerDate));
        if (!logFilePath.exists()) {
            logFilePath.mkdir();
        }
        String logFileName = logFilePath.getPath()
                .concat(File.separator)
                .concat(String.valueOf(logId))
                .concat(".log");
        return logFileName;
    }


    /**
     * 将日志记录到日志文件中
     *
     * @param fileName
     * @param content
     */
    public static void appendLog(String fileName, String content) {
        if (!StringUtil.hasText(fileName)) {
            return;
        }
        File file = new File(fileName);
        // 第一次写内容肯定不存在
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                return;
            }
        }
        content = content == null ? "" : content;
        content += "\r\n";
        // 调用BufferedOutputStream将内容写入文件
        write(file, content);
    }

    /**
     * 将指定日志文件中的内容读出
     * @param fileName
     * @param fromLineNum
     * @return
     */
    public static LogResult readLog(String fileName, int fromLineNum) {
        // 读取失败的逻辑
        if (!StringUtil.hasText(fileName)) {
            return new LogResult(fromLineNum, 0, "readLog fail, logFile not found", true);
        }
        File file = new File(fileName);
        if (!file.exists()) {
            return new LogResult(fromLineNum, 0, "readLog fail, logFile not exists", true);
        }
        StringBuffer logContentBuffer = new StringBuffer();
        int toLineNum = 0;
        LineNumberReader reader = null;
        try {
            reader = new LineNumberReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            String line = null;
            while ((line = reader.readLine()) != null) {
                toLineNum = reader.getLineNumber();
                if (toLineNum >= fromLineNum) {
                    logContentBuffer.append(line);
                }
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
        LogResult logResult = new LogResult(fromLineNum, toLineNum, logContentBuffer.toString(), false);
        return logResult;
    }










    private static void write(File file, String content) {
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        try {
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(content.getBytes(StandardCharsets.UTF_8));
            bos.flush();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return;
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                    return;
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                    return;
                }
            }

        }

    }


    public static String getLogBasePath() {
        return logBasePath;
    }

    public static void setLogBasePath(String logBasePath) {
        XxlJobFileAppender.logBasePath = logBasePath;
    }

    public static String getGlueSrcPath() {
        return glueSrcPath;
    }

    public static void setGlueSrcPath(String glueSrcPath) {
        XxlJobFileAppender.glueSrcPath = glueSrcPath;
    }
}
