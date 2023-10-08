package com.xiaohe.admin.core.thread;

import com.xiaohe.admin.core.conf.XxlJobAdminConfig;
import com.xiaohe.admin.core.trigger.TriggerTypeEnum;
import groovy.transform.AutoImplement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.assembler.AutodetectCapableMBeanInfoAssembler;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author : 小何
 * @Description : 数据库中查询任务后交给此组件处理，拥有快慢线程池，
 * JobScheduleHelper 从数据库中查询出定时任务，交给这个组件。
 * 这个组件拿到任务后先看看是不是慢执行任务，进而挑选线程池去执行，执行完记录执行时间，判断此次执行是不是一次慢执行，记录在 jobTimeoutMap 中。
 * 并且通过判断时间，让 jobTimeoutMap 每分钟的数据都不一样，达到 “一个任务，一分钟内慢执行次数达到10次就认定为慢执行任务。”
 * @date : 2023-10-08 22:35
 */
public class JobTriggerPoolHelper {
    private static Logger logger = LoggerFactory.getLogger(JobTriggerPoolHelper.class);

    private static JobTriggerPoolHelper helper = new JobTriggerPoolHelper();

    /**
     * 快线程池
     */
    private ThreadPoolExecutor fastTriggerPool = null;

    /**
     * 慢线程池
     */
    private ThreadPoolExecutor slowTriggerPool = null;

    /**
     * 供外界调用的停止方法
     */
    public static void toStop() {
        helper.stop();
    }

    /**
     * 供外界调用的启动方法
     */
    public static void toStart() {
        helper.start();
    }

    /**
     * 初始化快慢线程池
     */
    private void start() {
        fastTriggerPool = new ThreadPoolExecutor(
                10,
                XxlJobAdminConfig.getAdminConfig().getTriggerPoolFastMax(),
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread("xxl-job, admin JobTriggerPoolHelper-fastTriggerPool-" + r.hashCode());
                    }
                }
        );
        slowTriggerPool = new ThreadPoolExecutor(
                20,
                XxlJobAdminConfig.getAdminConfig().getTriggerPoolSlowMax(),
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2000),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread("xxl-job, admin JobTriggerPoolHelper-slowTriggerPool-" + r.hashCode());
                    }
                }
        );
    }

    /**
     * 停止快慢线程池
     */
    private void stop() {
        fastTriggerPool.shutdownNow();
        slowTriggerPool.shutdownNow();
        logger.info(">>>>>>>>> xxl-job trigger thread pool shutdown success.");
    }

    /**
     * 时间轮，如果有慢任务出现就会被记录在该Map中。
     * 慢任务 : 执行时间超过500ms
     * key : jobId
     * value : 一分钟内该任务慢执行的次数
     */
    private volatile ConcurrentHashMap<Integer, AtomicInteger> jobTimeoutCountMap = new ConcurrentHashMap<>();

    /**
     * 时间，慢线程一分钟刷新一次，用的就是这个变量刷新
     */
    private volatile long minTime = System.currentTimeMillis() / (60 * 1000);

    /**
     * 暴露给外界的调度方法
     * @param jobId 任务id
     * @param triggerType 调度类型, 默认为cron，也可以是固定频率、父任务、重试任务...
     * @param failRetryCount 任务剩余的失败重试次数
     * @param executorShardingParam 分片参数
     * @param executorParam 任务参数
     * @param addressList 有机会执行任务的执行器列表，在 XxlJobTrigger 中会使用调度策略在这些执行器中选一个
     */
    public static void trigger(final int jobId,
                               final TriggerTypeEnum triggerType,
                               final int failRetryCount,
                               final String executorShardingParam,
                               final String executorParam,
                               final String addressList) {
        helper.addTrigger(jobId, triggerType, failRetryCount, executorShardingParam, executorParam, addressList);
    }

    /**
     * 调度方法的内部实现
     */
    public void addTrigger(final int jobId,
                           final TriggerTypeEnum triggerType,
                           final int failRetryCount,
                           final String executorShardingParam,
                           final String executorParam,
                           final String addressList) {
        // 默认使用快线程池，如果Map中的慢执行次数大于10，使用慢线程池
        ThreadPoolExecutor triggerPool = fastTriggerPool;
        AtomicInteger jobTimeoutCount = jobTimeoutCountMap.get(jobId);
        if (jobTimeoutCount != null && jobTimeoutCount.get() > 10) {
            triggerPool = fastTriggerPool;
        }
        triggerPool.execute(() -> {
            // start用于记录此次执行花费时间
            long start = System.currentTimeMillis();
            try {
                // TODO 使用 XxlJobTrigger 调度任务
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                // 获取当前分钟，如果跟 minTime 不一样，说明记录慢执行的Map需要更新了
                long minTime_now = System.currentTimeMillis() / (60 * 1000);
                if (minTime_now != minTime) {
                    minTime = minTime_now;
                    jobTimeoutCountMap.clear();
                }
                // 如果任务执行花费超过500ms，记录一次慢执行
                long end = System.currentTimeMillis();
                if (end - start > 500) {
                    AtomicInteger timeoutCount = jobTimeoutCountMap.putIfAbsent(jobId, new AtomicInteger(0));
                    if (timeoutCount != null) {
                        timeoutCount.incrementAndGet();
                    }
                }
            }
        });

    }





}
