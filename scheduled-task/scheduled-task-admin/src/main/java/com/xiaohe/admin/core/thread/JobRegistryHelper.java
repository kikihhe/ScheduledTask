package com.xiaohe.admin.core.thread;

import com.xiaohe.admin.core.conf.XxlJobAdminConfig;
import com.xiaohe.admin.core.model.XxlJobGroup;
import com.xiaohe.admin.core.model.XxlJobRegistry;
import com.xiaohe.core.enums.RegistryConfig;
import com.xiaohe.core.handler.annotation.XxlJob;
import com.xiaohe.core.model.RegistryParam;
import com.xiaohe.core.model.Result;
import com.xiaohe.core.util.CollectionUtil;
import com.xiaohe.core.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-10-06 14:26
 */
public class JobRegistryHelper {
    private static Logger logger = LoggerFactory.getLogger(JobRegistryHelper.class);

    private static JobRegistryHelper jobRegistryHelper = new JobRegistryHelper();

    public static JobRegistryHelper getInstance() {
        return jobRegistryHelper;
    }

    /**
     * 这个线程池用于提供注册、移除服务，别的应用调用注册服务时将任务提交给线程池
     */
    private ThreadPoolExecutor registryOrRemoveThreadPool = null;

    /**
     * 定时(30s)清理过期执行器，从数据库中查各个执行器的最新注册时间。如果离现在超过90s，就清理掉
     * 只清理自动注册的
     */
    private Thread registryMonitorThread;

    /**
     * 该组件是否结束
     */
    private volatile boolean toStop = false;

    public void start() {
        // 初始化线程池
        initRegistryOrRemoveThreadPool();
        // 给定时清理过期执行器的线程赋予任务, 30s一次
        initRegistryMonitorThread();
        registryMonitorThread.setDaemon(true);
        registryMonitorThread.setName("xxl-job, admin JobRegistryMonitorHelper-registryMonitorThread");
        registryMonitorThread.start();

    }

    /**
     * 初始化线程池
     */
    private void initRegistryOrRemoveThreadPool() {
        registryOrRemoveThreadPool = new ThreadPoolExecutor(
                2,
                10,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2000),
                // thread factory
                (runnable) -> {
                    return new Thread("xxl-job, admin JobThreadMonitorHelper-registryOrRemoveThreadPool-" + runnable.hashCode());
                },
                // 拒绝策略：再次执行一次
                (runnable, threadPoolExecutor) -> {
                    runnable.run();
                    logger.warn(">>>>>>>>>> xxl-job, registry or remove too fast, match threadpool rejected handler(run now)");
                }
        );
    }

    /**
     * 定时清理过期执行器
     */
    private void initRegistryMonitorThread() {
        registryMonitorThread = new Thread(() -> {
            while (!toStop) {
                // 查找自动注册的执行器组。如果有，再删那些宕机执行器也不迟
                List<XxlJobGroup> groupList = XxlJobAdminConfig.getAdminConfig().getXxlJobGroupMapper().findByAddressType(0);
                if (!CollectionUtil.isEmpty(groupList)) {
                    // key : appname, value: 很多执行器IP
                    // 这个Map中的数据跟groupList中group的address不一样，这个Map里的值是从注册信息中找的没有宕机的数据。
                    // 接下来同步这两个集合, addressMap中的数据才是最终数据
                    Map<String, List<String>> addressMap = getMap();
                    for (XxlJobGroup group : groupList) {
                        List<String> registryList = addressMap.get(group.getAppname());
                        // 以逗号隔开的执行器地址
                        String addressListStr = "";
                        if (!CollectionUtil.isEmpty(registryList)) {
                            registryList.sort(null);
                            addressListStr = String.join(",", registryList);
                        }
                        // 设置到Map中
                        group.setAddressList(addressListStr);
                        group.setUpdateTime(new Date());
                    }
                    if (!groupList.isEmpty()) {
                        XxlJobAdminConfig.getAdminConfig().getXxlJobGroupMapper().updateBatch(groupList);
                    }
                }
                //线程在这里睡30秒，也就意味着检测周期为30秒
                try {
                    TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
                } catch (InterruptedException e) {
                    if (!toStop) {
                        logger.error(">>>>>>>>>>> xxl-job, job registry monitor thread error:{}", e);
                    }
                }
            }
            logger.info(">>>>>>>>>>> xxl-job, job registry monitor thread stop");
        });
    }


    /**
     * 1. 从数据库中查出所有自动注册的执行器组                     <br/>
     * 2. 删除90s内没有注册的注册信息(手动、自动全部删除)                        <br/>
     * 3. 获取90s内已经注册过的执行器(自动注册的)，根据appname分类返回                          <br/>
     *
     * @return Map   key : appname, value: 很多执行器IP
     */
    private Map<String, List<String>> getMap() {
        // key : appname, value: 很多执行器IP
        HashMap<String, List<String>> addressMap = new HashMap<>();
        // 如果不为空，说明有自动注册的执行器组
        // 查找90s没有注册的执行器地址, 这里没有区分手动注册还是自动注册
        List<Integer> ids =
                XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryMapper().findDead(RegistryConfig.DEAD_TIMEOUT, new Date());
        if (!CollectionUtil.isEmpty(ids)) {
            // 将这些久久没有更新数据库注册信息的执行器注册信息删除
            XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryMapper().removeDead(ids);
        }
        // 从数据库中查出没有死亡的执行器。只要自动注册的，手动注册的不要。因为要将这些执行器的IP与刚才查出的group里的地址同步。
        List<XxlJobRegistry> list =
                XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryMapper().findAll(RegistryConfig.DEAD_TIMEOUT, new Date());
        if (!CollectionUtil.isEmpty(list)) {
            for (XxlJobRegistry item : list) {
                if (!RegistryConfig.RegistType.EXECUTOR.name().equals(item.getRegistryGroup())) {
                    continue;
                }
                String appname = item.getRegistryKey();
                List<String> registryList = addressMap.get(appname);
                if (registryList == null) {
                    registryList = new ArrayList<>();
                }
                if (!registryList.contains(item.getRegistryValue())) {
                    registryList.add(item.getRegistryValue());
                }
                addressMap.put(appname, registryList);
            }
        }

        return addressMap;
    }

    /**
     * 停止这两个组件
     */
    public void toStop() {
        toStop = true;
        registryOrRemoveThreadPool.shutdownNow();
    }

    /**
     * 将指定的注册信息删除
     *
     * @param registryParam
     */
    public Result<String> registryRemove(RegistryParam registryParam) {
        if (!StringUtil.hasText(registryParam.getRegistryGroup())
                || !StringUtil.hasText(registryParam.getRegistryKey())
                || !StringUtil.hasText(registryParam.getRegistryValue())) {
            return Result.error("Illegal Argument.");
        }
        registryOrRemoveThreadPool.execute(() -> {
            XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryMapper().registryDelete(
                    registryParam.getRegistryGroup(),
                    registryParam.getRegistryKey(),
                    registryParam.getRegistryValue());
        });
        return Result.SUCCESS;
    }


    public Result<String> registry(RegistryParam registryParam) {
        if (!StringUtil.hasText(registryParam.getRegistryGroup())
                || !StringUtil.hasText(registryParam.getRegistryKey())
                || !StringUtil.hasText(registryParam.getRegistryValue())) {
            return Result.error("Illegal Argument.");
        }
        registryOrRemoveThreadPool.execute(() -> {
            // 默认这个心跳是刷新
            int update = XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryMapper().registryUpdate(
                    registryParam.getRegistryGroup(),
                    registryParam.getRegistryKey(),
                    registryParam.getRegistryValue(),
                    new Date()
            );
            // 没刷新成功就说明还没有，即这个执行器第一次心跳。注册一下
            if (update < 1) {
                XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryMapper().registrySave(
                        registryParam.getRegistryGroup(),
                        registryParam.getRegistryKey(),
                        registryParam.getRegistryValue(),
                        new Date()
                );
            }

        });
        return Result.SUCCESS;
    }


}



