package com.xiaohe.core.thread;

import com.xiaohe.core.biz.AdminBiz;
import com.xiaohe.core.enums.RegistryConfig;
import com.xiaohe.core.executor.XxlJobExecutor;
import com.xiaohe.core.model.RegistryParam;
import com.xiaohe.core.model.Result;
import com.xiaohe.core.util.CollectionUtil;
import com.xiaohe.core.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author : 小何
 * @Description : 执行器注册的线程
 * @date : 2023-10-02 12:47
 */
public class ExecutorRegistryThread {
    private static Logger logger = LoggerFactory.getLogger(ExecutorRegistryThread.class);
    private static ExecutorRegistryThread instance = new ExecutorRegistryThread();

    public static ExecutorRegistryThread getInstance() {
        return instance;
    }

    /**
     * 注册线程
     */
    private Thread registryThread;

    /**
     * 该组件是否结束
     */
    private volatile boolean toStop = false;

    /**
     * 开始本执行器的注册
     *
     * @param appname
     * @param address
     */
    public void start(final String appname, final String address) {
        // 检查
        if (!StringUtil.hasText(appname)) {
            logger.warn(">>>>>>>>>>>> xxl-job, executor registry config fail, appname is null");
            return;
        }
        if (CollectionUtil.isEmpty(XxlJobExecutor.getAdminBizList())) {
            logger.warn(">>>>>>>>>>>> xxl-job, executor registry config fail, adminAddresses is null");
            return;
        }
        // 给注册线程分配任务
        registryThread = new Thread(() -> {
            // 如果循环没有停止，就30s一次的注册。
            while (!toStop) {
                registry(appname, address);
            }
            // 出了循环，说明执行器停止，将此执行器在调度中心那里注销
            registryRemove(appname, address);
            logger.info(">>>>>>>>>>> xxl-job, executor registry thread destroy.");
        });

        registryThread.setDaemon(true);
        registryThread.setName("xxl-job, executor ExecutorRegistryThread");
        registryThread.start();
    }

    private void registry(String appname, String address) {
        try {
            RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistType.EXECUTOR.name(), appname, address);
            // 向所有调度中心注册，成功一个就结束
            for (AdminBiz adminBiz : XxlJobExecutor.getAdminBizList()) {
                Result registryResult = adminBiz.registry(registryParam);
                if (registryResult != null && registryResult.getCode() == Result.SUCCESS_CODE) {
                    registryResult = Result.SUCCESS;
                    logger.debug(">>>>>>>>>>> xxl-job registry success, registryParam:{}, registryResult:{}", registryParam, registryResult);
                    break;
                } else {
                    logger.info(">>>>>>>>>>> xxl-job registry fail, registryParam:{}, registryResult:{}", registryParam, registryResult);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        // 30s注册一次
        if (!toStop) {
            try {
                TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
            } catch (InterruptedException e) {
                logger.warn(">>>>>>>>>>>>>> xxl-job, executor registry thread interrupted, error msg: {}", e.getMessage());
            }
        }
    }

    private void registryRemove(String appname, String address) {
        // 出了循环说明执行器要终止了，向调度中心发送注销信息
        try {
            RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistType.EXECUTOR.name(), appname, address);
            for (AdminBiz adminBiz : XxlJobExecutor.getAdminBizList()) {
                Result removeResult = adminBiz.registryRemove(registryParam);
                if (removeResult != null && removeResult.getCode() == Result.SUCCESS_CODE) {
                    logger.debug(">>>>>>>>>> xxl-job registry-remove success, registryParam:{}, removeResult:{}", registryParam, removeResult);
                    break;
                } else {
                    logger.debug(">>>>>>>>>> xxl-job registry-remove fail, registryParam:{}, removeResult:{}", registryParam, removeResult);
                }
            }
        } catch (Exception e) {
            if (!toStop) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public void toStop() {
        toStop = true;
        if (registryThread != null) {
            // 如果线程正在阻塞，让它结束阻塞
            registryThread.interrupt();
            try {
                // 让线程接下来的动作执行完
                registryThread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }


}
