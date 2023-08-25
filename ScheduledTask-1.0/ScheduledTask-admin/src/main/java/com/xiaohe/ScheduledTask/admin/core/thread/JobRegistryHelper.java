package com.xiaohe.ScheduledTask.admin.core.thread;

import com.xiaohe.ScheduledTask.admin.core.conf.ScheduledTaskAdminConfig;
import com.xiaohe.ScheduledTask.admin.core.model.ScheduledTaskGroup;
import com.xiaohe.ScheduledTask.admin.core.model.ScheduledTaskRegistry;
import com.xiaohe.ScheduledTask.core.biz.model.RegistryParam;
import com.xiaohe.ScheduledTask.core.biz.model.Result;
import com.xiaohe.ScheduledTask.core.enums.RegistryConfig;
import com.xiaohe.ScheduledTask.core.util.CollectionUtil;
import com.xiaohe.ScheduledTask.core.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author : 小何
 * @Description : 负责执行器的注册、移除、检查宕机执行器
 * 内部有一个线程池 + 一个线程，线程池用于执行器注册、移除。线程用于定期清理宕机执行器(只清理自动注册的执行器)
 * @date : 2023-08-25 11:56
 */
public class JobRegistryHelper {
    private static Logger logger = LoggerFactory.getLogger(JobRegistryHelper.class);
    private static JobRegistryHelper instance = new JobRegistryHelper();

    public static JobRegistryHelper getInstance() {
        return instance;
    }

    /**
     * 用于执行器的注册、移除
     */
    private ThreadPoolExecutor registryOrRemoveThreadPool = null;

    /**
     * 检测宕机执行器的线程
     */
    private Thread registryMonitorThread = null;

    /**
     * 本类是否停止工作
     */
    private volatile boolean toStop = false;

    /**
     * 初始化线程池registryOrRemoveThreadPool，
     * 给线程registryMonitorThread指定任务: 扫描数据库中下线的执行器
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
                        return new Thread(r, "ScheduledTask, admin JobRegistryMonitorHelper-registryOrRemoveThreadPool-" + r.hashCode());
                    }
                },
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        // 所谓的拒绝就是把任务再执行一遍
                        r.run();
                        logger.warn(">>>>>>>>>>> ScheduledTask, registry or remove too fast, match threadpool rejected handler(run now).");
                    }
                });
        // 该线程每30s检测一次过期执行器
        registryMonitorThread = new Thread(() -> {
            while (!toStop) {
                try {
                    // 查找所有自动注册的执行器组, 再查出已下线的执行器将其删除，
                    // 再查出未删除的执行器，将未删除的执行器按照appname(也就是group)塞到groupList中。
                    List<ScheduledTaskGroup> groupList = ScheduledTaskAdminConfig.getAdminConfig().getScheduledTaskGroupMapper().findByAddressType(0);
                    if (!CollectionUtil.isEmpty(groupList)) {
                        // 查出所有90s未更新的执行器
                        List<Integer> ids = ScheduledTaskAdminConfig.getAdminConfig().getScheduledTaskRegistryMapper().findDead(RegistryConfig.DEAD_TIMEOUT, new Date());
                        // 直接删除这些已下线的执行器
                        if (!CollectionUtil.isEmpty(ids)) {
                            ScheduledTaskAdminConfig.getAdminConfig().getScheduledTaskRegistryMapper().removeDead(ids);
                        }
                        // 该Map用于缓存 appname 与其对应的执行器地址
                        Map<String, List<String>> appAddressMap = new HashMap<>();
                        // 查出所有没下线的执行器
                        List<ScheduledTaskRegistry> list = ScheduledTaskAdminConfig.getAdminConfig().getScheduledTaskRegistryMapper().findAll(RegistryConfig.DEAD_TIMEOUT, new Date());
                        // 将这些未下线的执行器存入Map中
                        list.forEach(item -> {
                            String appname = item.getRegistryKey();
                            String ip = item.getRegistryValue();
                            List<String> addresses = appAddressMap.get(appname);
                            if (addresses == null) {
                                addresses = new ArrayList<>();
                            }
                            if (!addresses.contains(ip)) {
                                addresses.add(item.getRegistryValue());
                            }
                            appAddressMap.put(appname, addresses);
                        });
                        groupList.forEach(group -> {
                            String appName = group.getAppname();
                            List<String> appNameIps = appAddressMap.get(appName);
                            if (!CollectionUtil.isEmpty(appNameIps)) {
                                String addresses = appNameIps.stream().sorted().collect(Collectors.joining(","));
                                group.setAddressList(addresses);
                            }
                            group.setUpdateTime(new Date());
                        });
                        ScheduledTaskAdminConfig.getAdminConfig().getScheduledTaskGroupMapper().updateBatch(groupList);


                    }
                } catch (Exception e) {
                    if (!toStop) {
                        logger.error(">>>>>>>>>>> ScheduledTask, job registry monitor thread error:{}", e);
                    }
                }
                // 停30s继续检查
                try {
                    TimeUnit.SECONDS.sleep(30);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }


            }
            // 出了循环代表 job registry monitor thread停止
            logger.info(">>>>>>>>>>> ScheduledTask, job registry monitor thread stop");

        });
        registryMonitorThread.setDaemon(true);
        registryMonitorThread.setName("ScheduledTask, admin JobRegistryMonitorHelper-registryMonitorThread");
        registryMonitorThread.start();


    }

    /**
     * 停止 执行器的注册、清理
     */
    public void toStop() {
        toStop = true;
        registryOrRemoveThreadPool.shutdownNow();
    }


    /**
     * 注册执行器、更新执行器的有效期
     *
     * @param registryParam
     * @return
     */
    public Result registry(RegistryParam registryParam) {
        String registryGroup = registryParam.getRegistryGroup();
        String appname = registryParam.getRegistryKey();
        String ip = registryParam.getRegistryValue();
        if (!StringUtil.hasText(registryGroup)
                || !StringUtil.hasText(appname)
                || !StringUtil.hasText(ip)) {
            return new Result<String>(Result.FAIL_CODE, "Illegal Argument");
        }
        registryOrRemoveThreadPool.execute(() -> {
            int ret = ScheduledTaskAdminConfig.getAdminConfig().getScheduledTaskRegistryMapper().registryUpdate(registryGroup, appname, ip, new Date());
            // 更新执行器，更新失败代表数据库没有这个执行器，开始注册
            if (ret < 1) {
                ScheduledTaskAdminConfig.getAdminConfig().getScheduledTaskRegistryMapper().registrySave(registryGroup, appname, ip, new Date());
            }
        });
        return Result.SUCCESS;
    }


    /**
     * 移除指定的执行器
     *
     * @param registryParam
     * @return
     */
    public Result registryRemove(RegistryParam registryParam) {
        String registryGroup = registryParam.getRegistryGroup();
        String appname = registryParam.getRegistryKey();
        String ip = registryParam.getRegistryValue();
        if (!StringUtil.hasText(registryGroup)
                || !StringUtil.hasText(appname)
                || !StringUtil.hasText(ip)) {
            return new Result<String>(Result.FAIL_CODE, "Illegal Argument");
        }
        // 将任务提交给线程池来处理
        registryOrRemoveThreadPool.execute(() -> {
            ScheduledTaskAdminConfig.getAdminConfig().getScheduledTaskRegistryMapper().registryDelete(registryGroup, appname, ip);
        });
        return Result.SUCCESS;

    }


}
