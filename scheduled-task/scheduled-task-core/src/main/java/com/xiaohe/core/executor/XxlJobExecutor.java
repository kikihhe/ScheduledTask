package com.xiaohe.core.executor;

import com.xiaohe.core.biz.AdminBiz;
import com.xiaohe.core.biz.client.AdminBizClient;
import com.xiaohe.core.log.XxlJobFileAppender;
import com.xiaohe.core.thread.JobLogFileCleanThread;
import com.xiaohe.core.thread.TriggerCallbackThread;
import com.xiaohe.core.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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

        // 初始化执行器给调度中心发送消息的组件
        initAdminBizList(adminAddresses, accessToken);

        // 定时清理过期日志，一天一次
        JobLogFileCleanThread.getInstance().start(logRetentionDays);

        // 启动回调执行结果给调度中心的组件
        TriggerCallbackThread.getInstance().start();

    }


    // ------------------------------------------------------------------------------------------
    /**
     * 执行器给调度中心发送消息的组件
     */
    private static List<AdminBiz> adminBizList;




    /**
     * 初始化执行器给调度中心发送消息的组件
     * @param adminAddresses
     * @param accessToken
     */
    public void initAdminBizList(String adminAddresses, String accessToken) {
        if (!StringUtil.hasText(adminAddresses)) {
            return;
        }
        for (String address : adminAddresses.split(",")) {
            if (!StringUtil.hasText(address)) {
                continue;
            }
            AdminBizClient adminBiz = new AdminBizClient(address, accessToken);
            if (adminBizList == null) {
                adminBizList = new ArrayList<>();
            }
            adminBizList.add(adminBiz);
        }
    }




    // ------------------------------------------------------------------------------------------
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

    public static List<AdminBiz> getAdminBizList() {
        return adminBizList;
    }

    public static void setAdminBizList(List<AdminBiz> adminBizList) {
        XxlJobExecutor.adminBizList = adminBizList;
    }
}
