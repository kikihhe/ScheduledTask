package com.xiaohe.admin.core.scheduler;

import com.xiaohe.admin.core.conf.XxlJobAdminConfig;
import com.xiaohe.admin.core.thread.*;
import com.xiaohe.admin.core.util.I18nUtil;
import com.xiaohe.core.biz.ExecutorBiz;
import com.xiaohe.core.biz.client.ExecutorBizClient;
import com.xiaohe.core.enums.ExecutorBlockStrategyEnum;
import com.xiaohe.core.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : 小何
 * @Description : 调度中心 可以启动xxl-job的几个组件，可以获取与执行器通信的ExecutorBiz
 * @date : 2023-10-05 19:45
 */
public class XxlJobScheduler {
    private static Logger logger = LoggerFactory.getLogger(XxlJobScheduler.class);

    /**
     * 给所有执行器发送消息的客户端
     */
    private static ConcurrentHashMap<String, ExecutorBiz> executorBizRepository = new ConcurrentHashMap<>();

    public void start() {
        initI18n();
        // 快慢线程池
        JobTriggerPoolHelper.toStart();

        // 开始接收注册
        JobRegistryHelper.getInstance().start();

        // 任务失败告警组件
        JobFailMonitorHelper.getInstance().start();

        // 接收回调信息
        JobCompleteHelper.getInstance().start();

        // 定时统计日志，定时删除数据库中的日志
        JobLogReportHelper.getInstance().start();

        // 从数据库中选择数据执行
        JobScheduleHelper.getInstance().start();
    }

    public void destroy() {
        JobScheduleHelper.getInstance().toStop();
        JobLogReportHelper.getInstance().toStop();
        JobCompleteHelper.getInstance().toStop();
        JobFailMonitorHelper.getInstance().toStop();
        JobRegistryHelper.getInstance().toStop();
        JobTriggerPoolHelper.toStop();
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


    private void initI18n(){
        for (ExecutorBlockStrategyEnum item: ExecutorBlockStrategyEnum.values()) {
            item.setTitle(I18nUtil.getString("jobconf_block_".concat(item.name())));
        }
    }
}
