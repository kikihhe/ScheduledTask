package com.xiaohe.core.executor;

import com.xiaohe.core.log.XxlJobFileAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author : 小何
 * @Description : 执行器核心类
 * @date : 2023-09-28 20:57
 */
public class XxlJobExecutor {
    private static final Logger logger = LoggerFactory.getLogger(XxlJobExecutor.class);

    /**
     * 调度中心地址，以逗号隔开.
     * 执行器需要与调度中心通信
     */
    private String adminAddresses;

    /**
     * 通信需要使用到的token
     */
    private String accessToken;

    /**
     * 执行器的名称，执行器注册到调度中心时使用它
     */
    private String appname;

    /**
     * 执行器的IP地址
     */
    private String address;

    /**
     * 执行器端口
     */
    private int port;

    /**
     * 用户指定的日志文件地址
     */
    private String logPath;

    /**
     * 日志最大保存时间
     */
    private int logRetentionDays;

    /**
     * 执行器的启动方法
     * @throws Exception
     */
    public void start() throws Exception {
        // 指定日志文件的存放位置
        XxlJobFileAppender.initLogPath(logPath);

    }




    // ----------------------------------------------------------------------------------
    // 这些setter都是用户在config中配置的，毕竟用户需要指定日志文件放在哪里。
    public void setAdminAddresses(String adminAddresses) {
        this.adminAddresses = adminAddresses;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public void setLogRetentionDays(int logRetentionDays) {
        this.logRetentionDays = logRetentionDays;
    }
}
