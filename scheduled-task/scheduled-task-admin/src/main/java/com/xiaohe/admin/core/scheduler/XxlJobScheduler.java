package com.xiaohe.admin.core.scheduler;

import com.xiaohe.admin.core.conf.XxlJobAdminConfig;
import com.xiaohe.core.biz.ExecutorBiz;
import com.xiaohe.core.biz.client.ExecutorBizClient;
import com.xiaohe.core.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : 小何
 * @Description : 调度中心
 * @date : 2023-10-05 19:45
 */
public class XxlJobScheduler {
    private static Logger logger = LoggerFactory.getLogger(XxlJobScheduler.class);

    /**
     * 给所有执行器发送消息的客户端
     */
    private static ConcurrentHashMap<String, ExecutorBiz> executorBizRepository = new ConcurrentHashMap<>();

    public void start() {

    }

    public void destroy() {

    }


    /**
     * 根据执行器的地址，获取与该执行器通信的工具
     * @param address
     * @return
     */
    public static ExecutorBiz getExecutorBiz(String address) {
        if (!StringUtil.hasText(address)) {
            return null;
        }
        address = address.trim();
        ExecutorBiz executorBiz = executorBizRepository.get(address);
        if (executorBiz != null) {
            return executorBiz;
        }
        executorBiz = new ExecutorBizClient(address, XxlJobAdminConfig.getAdminConfig().getAccessToken());
        executorBizRepository.put(address, executorBiz);
        return executorBiz;
    }



}
