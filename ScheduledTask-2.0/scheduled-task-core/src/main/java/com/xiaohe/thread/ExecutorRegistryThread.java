package com.xiaohe.thread;

import com.xiaohe.biz.AdminBiz;
import com.xiaohe.biz.model.RegistryParam;
import com.xiaohe.biz.model.Result;
import com.xiaohe.enums.RegistryConfig;
import com.xiaohe.executor.ScheduledTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * @author : 小何
 * @Description : 执行器进行注册的类，注册、心跳
 * @date : 2023-09-03 14:26
 */
public class ExecutorRegistryThread {
    private static Logger logger = LoggerFactory.getLogger(ExecutorRegistryThread.class);

    private static ExecutorRegistryThread instance = new ExecutorRegistryThread();
    public static ExecutorRegistryThread getInstance() {
        return instance;
    }


    /**
     * 将执行器注册到调度中心的线程
     */
    private Thread registryThread;

    /**
     * registryThread停止工作的标识
     */
    private volatile boolean toStop = false;

    /**
     * 将执行器注册到调度中心
     * @param appname
     * @param address
     */
    public void start(final String appname, final String address) {
        if (!StringUtils.hasText(appname)) {
            logger.warn("executor registry config fail, appname is null.");
            return;
        }

        // 如果客户端没法和调度中心通信，那还注册啥
        if (CollectionUtils.isEmpty(ScheduledTaskExecutor.getAdminBizList())) {
            logger.warn("executor registry config fail, adminAddresses is null.");
            return;
        }

        registryThread = new Thread(() -> {
            while (!toStop) {
                // 创建注册参数，将注册参数发给调度中心。调度中心可能是集群，需要遍历所有客户端，但是只要有一个注册成功就不需要继续遍历
                RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistType.EXECUTOR.name(), appname, address);
                for (AdminBiz adminBiz : ScheduledTaskExecutor.getAdminBizList()) {
                    Result<String> registryResult = adminBiz.registry(registryParam);
                    if (registryResult != null && Result.SUCCESS_CODE == registryResult.getCode()) {
                        registryResult = Result.SUCCESS;
                        break;
                    } else {
                        // 注册失败，打日志，下一个
                        logger.info("registry fail, registryParam:{}, registryResult:{}", registryParam, registryResult);
                    }

                }
                // 30s注册一次
                try {
                    TimeUnit.SECONDS.sleep(30);
                } catch (InterruptedException e) {
                    if (!toStop) {
                        logger.warn(">>>>>>>>>>> scheduled-task, executor registry thread interrupted, error msg:{}", e.getMessage());
                    }
                }

            }
            // 走到这里说明 toStop 为false, 用户想要停止注册线程。所以将删除此执行器的消息发送给调度中心
            RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistType.EXECUTOR.name(), appname, address);
            for (AdminBiz adminBiz : ScheduledTaskExecutor.getAdminBizList()) {
                Result<String> removeResult = adminBiz.registryRemove(registryParam);
                if (removeResult != null && removeResult.getCode() == Result.SUCCESS_CODE) {
                    removeResult = Result.SUCCESS;
                    break;
                } else {
                    logger.info(">>>>>>>>>>> scheduled-task registry-remove fail, registryParam:{}, registryResult:{}", new Object[]{registryParam, removeResult});
                }

            }
            logger.info(">>>>>>>>>>> scheduled-task, executor registry thread destroy.");


        });
        registryThread.setDaemon(true);
        registryThread.setName("executor ExecutorRegistryThread");
        registryThread.start();
    }


    /**
     * 终止注册线程
     */
    public void toStop() {
        toStop = true;
        if (registryThread != null) {
            registryThread.interrupt();
            try {
                registryThread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }






}
