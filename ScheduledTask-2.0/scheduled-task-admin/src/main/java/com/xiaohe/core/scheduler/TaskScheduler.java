package com.xiaohe.core.scheduler;

import com.xiaohe.biz.ExecutorBiz;
import com.xiaohe.biz.client.ExecutorBizClient;
import com.xiaohe.core.conf.ScheduledTaskAdminConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : 小何
 * @Description :
 *      容器的启动类，init方法中初始化各种组件。destroy方法中停止各种组件。
 *      同时包含调度中心向执行器发送消息的客户端。
 *
 * @date : 2023-09-01 10:20
 */
public class TaskScheduler {

    private static Logger logger = LoggerFactory.getLogger(TaskScheduler.class);

    /**
     * 初始化各种组件
     * @throws Exception
     */
    public void init() throws Exception {

    }

    /**
     * 销毁各种组件
      * @throws Exception
     */
    public void destroy() throws Exception {

    }

    private static ConcurrentHashMap<String, ExecutorBiz> executorBizRepository = new ConcurrentHashMap<>();


    /**
     * 获取调度中心给执行器发送消息的客户端
     * @param address 执行器的IP地址
     * @return
     */
    public static ExecutorBiz getExecutorBiz(String address) {
        if (address == null || address.trim().isEmpty()) {
            return null;
        }
        address = address.trim();
        ExecutorBiz executorBiz = executorBizRepository.get(address);
        if (executorBiz != null) {
            return executorBiz;
        }
        // 创建新的通信工具
        executorBiz = new ExecutorBizClient(address, ScheduledTaskAdminConfig.getAdminConfig().getAccessToken());
        executorBizRepository.put(address, executorBiz);
        return executorBiz;
    }



}
