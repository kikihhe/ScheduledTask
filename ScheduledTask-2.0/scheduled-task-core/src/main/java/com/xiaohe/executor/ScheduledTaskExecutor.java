package com.xiaohe.executor;

import com.xiaohe.biz.AdminBiz;
import com.xiaohe.biz.client.AdminBizClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author : 小何
 * @Description : 执行器的启动类
 * @date : 2023-09-02 10:45
 */
public class ScheduledTaskExecutor {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskExecutor.class);

    /**
     * 所有调度中心的地址，以逗号隔开
     */
    private String adminAddresses;

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 执行器组的名称
     */
    private String appname;

    /**
     * 执行器的地址 ip + port
     */
    private String address;

    /**
     * 执行器的IP
     */
    private String ip;

    /**
     * 执行器部署端口
     */
    private int port;

    /**
     * 日志收集路径
     */
    private String logPath;

    /**
     * 日志保留天数
     */
    private int logRetentionDays;

    // 从这里开始，就是组件重要的成员变量 <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

    /**
     * 执行器给调度中心发送消息的客户端
     */
    private static List<AdminBiz> adminBizList;



    /**
     * 启动执行器的各个组件               <br></br>
     * 如: 日志收集组件、执行器与调度中心通信的客户端组件、执行结果回调组件、执行器服务端组件
     */
    public void start() {

        // 初始化所有 执行器给调度中心发送消息 的客户端
        initAdminBizList(adminAddresses, accessToken);


    }


    /**
     * 初始化所有 执行器给调度中心发送消息 的客户端
     * @param adminAddresses
     * @param accessToken
     */
    private void initAdminBizList(String adminAddresses, String accessToken) {
        if (adminAddresses == null && adminAddresses.trim().isEmpty()) {
            return;
        }
        String[] addressList = adminAddresses.trim().split(",");
        for (String address : addressList) {
            if (address != null && !address.trim().isEmpty()) {
                AdminBizClient adminBiz = new AdminBizClient(address.trim(), accessToken);
                if (adminBizList.isEmpty()) {
                    adminBizList = new ArrayList<>();
                }
                adminBizList.add(adminBiz);
            }
        }
    }


    public static List<AdminBiz> getAdminBizList() {
        return adminBizList;
    }

    // 下面这些set方法会在 ScheduledTaskConfig类中调用，给ScheduledTaskExecutor内的属性赋值
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
    public void setIp(String ip) {
        this.ip = ip;
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
