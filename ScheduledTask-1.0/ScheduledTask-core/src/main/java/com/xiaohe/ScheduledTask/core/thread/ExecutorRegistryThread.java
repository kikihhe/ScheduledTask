package com.xiaohe.ScheduledTask.core.thread;

import com.xiaohe.ScheduledTask.core.biz.AdminBiz;
import com.xiaohe.ScheduledTask.core.biz.model.RegistryParam;
import com.xiaohe.ScheduledTask.core.biz.model.Result;
import com.xiaohe.ScheduledTask.core.executor.ScheduledTaskExecutor;
import com.xiaohe.ScheduledTask.core.util.ObjectUtil;
import com.xiaohe.ScheduledTask.core.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author : 小何
 * @Description : 执行器 自动注册、维持心跳 的类
 * @date : 2023-08-24 20:11
 */
public class ExecutorRegistryThread {
    private static Logger logger = LoggerFactory.getLogger(ExecutorRegistryThread.class);

    private static ExecutorRegistryThread instance = new ExecutorRegistryThread();

    public static ExecutorRegistryThread getInstance() {
        return instance;
    }

    /**
     * 干活的线程
     */
    private Thread registryThread;

    /**
     * 线程是否终止的标记
     */
    private volatile boolean toStop = false;

    /**
     * 终止线程
     */
    public void toStop() {
        toStop = true;
        // 线程不为空，中断注册线程
        if (ObjectUtil.isNotNull(registryThread != null)) {
            registryThread.interrupt();
            try {
                // 先让注册线程执行完再执行此线程。
                registryThread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }

        }

    }

    /**
     * 启动注册线程, 将appname于address发送给调度中心
     *
     * @param appname 执行器的appname
     * @param address 执行器的地址
     */
    public void start(final String appname, final String address) {
        if (!StringUtil.hasText(appname)) {
            logger.warn(">>>>>>>>>>> ScheduledTask, executor registry config fail, appname is null.");
        }
        // 如果执行器跟调度中心通信的list集合为空，说明没配置
        List<AdminBiz> adminBizList = ScheduledTaskExecutor.getAdminBizList();
        if (adminBizList == null) {
            logger.warn(">>>>>>>>>>> ScheduledTask, executor registry config fail, adminAddresses is null.");
            return;
        }
        registryThread = new Thread(() -> {
            while (!toStop) {
                try {
                    RegistryParam registryParam = new RegistryParam("", appname, address);
                    // 向每一个调度中心都注册
                    for (AdminBiz adminBiz : adminBizList) {
                        try {
                            Result<String> registryResult = adminBiz.registry(registryParam);
                            // 注册成功就可以跳出循环了，因为只要有一个调度中心收到并成功处理，就将这个执行器写入数据库了
                            if (ObjectUtil.isNotNull(registryResult) && Result.SUCCESS_CODE == registryResult.getCode()) {
                                registryResult = Result.SUCCESS;
                                logger.debug(">>>>>>>>>>> ScheduledTask registry success, registryParam:{}, registryResult:{}", new Object[]{registryParam, registryResult});
                                break;
                            } else {
                                logger.info(">>>>>>>>>>> ScheduledTask registry fail, registryParam:{}, registryResult:{}", new Object[]{registryParam, registryResult});
                            }
                        } catch (Exception e) {
                            // 避免出现网络等问题，如果在下面捕获，就无法继续循环，所以在这里捕获打日志
                            logger.info(">>>>>>>>>>> ScheduledTask registry error, registryParam:{}", registryParam, e);
                        }

                    }
                } catch (Exception e) {
                    // 如果出现异常，但我们还没有停止线程，记录日志
                    if (!toStop) {
                        logger.error(e.getMessage(), e);
                    }
                }
                // 每隔30s重新注册，心跳检测机制
                try {
                    if (!toStop) {
                        TimeUnit.SECONDS.sleep(30);
                    }
                } catch (InterruptedException e) {
                    if (!toStop) {
                        logger.error(e.getMessage(), e);
                    }
                }


            }

            // 如果出了while循环，代表线程被手动终止了，即此执行器不再提供服务，就向调度中心发送消息，将这个执行器删除
            RegistryParam registryParam = new RegistryParam("", appname, address);
            for (AdminBiz adminBiz : adminBizList) {
                try {
                    Result<String> removeResult = adminBiz.registryRemove(registryParam);
                    // 删除成功，切断循环
                    if (ObjectUtil.isNotNull(removeResult) && Result.SUCCESS_CODE == removeResult.getCode()) {
                        removeResult = Result.SUCCESS;
                        logger.info(">>>>>>>>>>> ScheduledTask registry-remove success, registryParam:{}, registryResult:{}", new Object[]{registryParam, removeResult});
                        break;
                    } else {
                        logger.info(">>>>>>>>>>> ScheduledTask registry-remove fail, registryParam:{}, registryResult:{}", new Object[]{registryParam, removeResult});
                    }
                } catch (Exception e) {
                    if (!toStop) {
                        logger.info(">>>>>>>>>>> ScheduledTask registry-remove error, registryParam:{}", registryParam, e);
                    }
                }
            }
            logger.info(">>>>>>>>>>> ScheduledTask, executor registry thread destroy.");
        });

        // 启动线程
        registryThread.setDaemon(true);
        registryThread.setName("ScheduledTask, executor ExecutorRegistryThread");
        registryThread.start();

    }


}
