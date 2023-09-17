package com.xiaohe.core.thread;


import com.xiaohe.biz.model.RegistryParam;
import com.xiaohe.biz.model.Result;
import com.xiaohe.core.conf.ScheduledTaskAdminConfig;
import com.xiaohe.core.model.ScheduledTaskGroup;
import com.xiaohe.core.model.ScheduledTaskRegistry;
import com.xiaohe.enums.RegistryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.*;

/**
 * @author : 小何
 * @Description : 执行器注册、移除事件的处理，定时检测执行器的健康状态
 * @date : 2023-09-05 13:47
 */
public class JobRegistryHelper {
    private static Logger logger = LoggerFactory.getLogger(JobRegistryHelper.class);

    private static JobRegistryHelper jobRegistryHelper = new JobRegistryHelper();

    public static JobRegistryHelper getInstance() {
        return jobRegistryHelper;
    }

    /**
     * 线程池，用于处理执行器发送的注册、移除信息
     */
    private ThreadPoolExecutor registryOrRemoveThreadPool;

    /**
     * 定时向从数据库查询执行器的健康状态，超过90s没有更新视为宕机，移除
     */
    private Thread registryMonitorThread;

    private volatile boolean toStop;

    /**
     * 启动线程、初始化线程池
     */
    public void start() {
        registryOrRemoveThreadPool = new ThreadPoolExecutor(
                2,
                10,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(2000),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "scheduled-task, admin JobRegistryMonitorHelper-registryOrRemoveThreadPool-" + r.hashCode());
                    }
                },
                //下面这个是xxl-job定义的线程池拒绝策略，其实就是把被拒绝的任务再执行一遍
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        //在这里能看到，所谓的拒绝，就是把任务再执行一遍
                        r.run();
                        logger.warn(">>>>>>>>>>> scheduled-task registry or remove too fast, match threadpool rejected handler(run now).");
                    }
                });

        // 心跳检测线程每30s检测一次执行器的健康状态(从数据库里面查，距离现在90s没有更新状态的执行器视为宕机，移除)
        // 只检测自动注册的执行器组
        registryMonitorThread = new Thread(() -> {
            while (!toStop) {
                // 从数据库中找出自动注册的执行器组, 如果没有自动注册的组，就结束了
                List<ScheduledTaskGroup> groupList = ScheduledTaskAdminConfig.getAdminConfig().getScheduledTaskGroupMapper().findByAddressType(0);
                if (CollectionUtils.isEmpty(groupList)) {
                    return;
                }
                // 找出需要删除的执行器
                List<Integer> ids = ScheduledTaskAdminConfig.getAdminConfig().getScheduledTaskRegistryMapper().findDead(RegistryConfig.DEAD_TIMEOUT, new Date());
                if (!CollectionUtils.isEmpty(ids)) {
                    ScheduledTaskAdminConfig.getAdminConfig().getScheduledTaskRegistryMapper().removeDead(ids);
                }
                // 删除后，group表的addressList字段执行器就有可能跟registry表中的不一样，开始更新group中的addressList字段
                Map<String, List<String>> appAddressMap = new HashMap<>();
                // 查出registry表所有(未宕机)执行器(自动注册、手动注册)
                List<ScheduledTaskRegistry> list = ScheduledTaskAdminConfig.getAdminConfig().getScheduledTaskRegistryMapper().findAll(RegistryConfig.DEAD_TIMEOUT, new Date());
                if (!CollectionUtils.isEmpty(list)) {
                    for (ScheduledTaskRegistry item : list) {
                        // 如果是手动注册，跳过
                        if (!RegistryConfig.RegistType.EXECUTOR.name().equals(item.getRegistryGroup())) {
                            continue;
                        }
                        // 如果是自动注册，按照appname将他们分好组
                        String appname = item.getRegistryKey();
                        List<String> addressList = appAddressMap.computeIfAbsent(appname, k -> new ArrayList<>());
                        if (!addressList.contains(item.getRegistryValue())) {
                            addressList.add(item.getRegistryValue());
                        }

                    }
                }
                // 将Map中的数据同步到group中，刷新到数据库
                for (ScheduledTaskGroup group : groupList) {
                    List<String> addressList = appAddressMap.get(group.getAppname());
                    Collections.sort(addressList);
                    String addressListStr = String.join(",", addressList);
                    group.setAddressList(addressListStr);
                    group.setUpdateTime(new Date());
                    ScheduledTaskAdminConfig.getAdminConfig().getScheduledTaskGroupMapper().update(group);
                }
                // 每隔30s刷新一次
                try {
                    TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
                } catch (InterruptedException e) {
                    if (!toStop) {
                        logger.error(">>>>>>>>>>> scheduled-task, job registry monitor thread error:{}", e);
                    }
                }

            }

        });

        registryMonitorThread.setDaemon(true);
        registryMonitorThread.setName("scheduled-task, admin JobRegistryMonitorHelper-registryMonitorThread");
        registryMonitorThread.start();


    }



    /**
     * 执行器的注册
     *
     * @param registryParam
     */
    public Result<String> registry(RegistryParam registryParam) {
        if (!StringUtils.hasText(registryParam.getRegistryGroup())
                || !StringUtils.hasText(registryParam.getRegistryKey())
                || !StringUtils.hasText(registryParam.getRegistryValue())) {
            return new Result<>(Result.FAIL_CODE, "Illegal Argument");
        }
        registryOrRemoveThreadPool.execute(() -> {
            // 先去更新，修改行数为0证明执行器还未注册，让它去注册
            int update = ScheduledTaskAdminConfig.getAdminConfig().getScheduledTaskRegistryMapper()
                    .registryUpdate(registryParam.getRegistryGroup(), registryParam.getRegistryKey(), registryParam.getRegistryValue(), new Date());
            if (update < 1) {
                ScheduledTaskAdminConfig.getAdminConfig().getScheduledTaskRegistryMapper().registrySave(registryParam, new Date());
                freshGroupRegistryInfo(registryParam);
            }

        });
        return Result.SUCCESS;
    }

    /**
     * 删除执行器
     * @param registryParam
     * @return
     */
    public Result<String> registryRemove(RegistryParam registryParam) {
        if (!StringUtils.hasText(registryParam.getRegistryGroup())
                || !StringUtils.hasText(registryParam.getRegistryKey())
                || !StringUtils.hasText(registryParam.getRegistryValue())) {
            return new Result<>(Result.FAIL_CODE, "Illegal Argument");
        }
        registryOrRemoveThreadPool.execute(() -> {
            int delete = ScheduledTaskAdminConfig.getAdminConfig().getScheduledTaskRegistryMapper().registryDelete(registryParam.getRegistryGroup(), registryParam.getRegistryKey(), registryParam.getRegistryValue());
            // 删除成功更新group表
            if (delete > 0) {
                freshGroupRegistryInfo(registryParam);
            }
        });
        return Result.SUCCESS;
    }

    /**
     * 在新增/修改/删除 registry表中的数据后，将group表中的数据与之同步
     * @param registryParam
     */
    private void freshGroupRegistryInfo(RegistryParam registryParam) {

    }

    /**
     * 关闭 JobRegistryHelper, 停止执行器的更新、注册、移除、心跳接收
     */
    public void toStop() {
        toStop = true;
        registryOrRemoveThreadPool.shutdownNow();
    }
}
