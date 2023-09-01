package com.xiaohe.core.thread;

import com.xiaohe.core.conf.ScheduledTaskAdminConfig;
import com.xiaohe.core.trigger.ScheduledTaskTrigger;
import com.xiaohe.core.trigger.TriggerTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author : 小何
 * @Description : 快慢线程池
 * @date : 2023-09-01 16:35
 */
public class JobTriggerPoolHelper {

    private static Logger logger = LoggerFactory.getLogger(JobTriggerPoolHelper.class);

    private static JobTriggerPoolHelper jobTriggerPoolHelper = new JobTriggerPoolHelper();


    /**
     * 快线程池 (默认)
     */
    private ThreadPoolExecutor fastTriggerPool = null;

    /**
     * 慢线程池             <br></br>
     * 一个任务的执行时间超过0.5s视为慢执行，一个任务在一分钟内慢执行次数超过10次视为慢任, 交给慢线程池处理
     */
    private ThreadPoolExecutor slowTriggerPool = null;


    /**
     * 供外界调用的开始接口
     */
    public static void toStart() {
        jobTriggerPoolHelper.start();

    }

    public static void toStop() {
        jobTriggerPoolHelper.stop();
    }

    /**
     * 初始化两个线程池
     */
    public void start() {
        fastTriggerPool = new ThreadPoolExecutor(
                10,
                ScheduledTaskAdminConfig.getAdminConfig().getTriggerPoolFastMax(),
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(1000),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "scheduled_task, admin JobTriggerPoolHelper-fastTriggerPool-" + r.hashCode());
                    }
                }

        );
        slowTriggerPool = new ThreadPoolExecutor(
                10,
                ScheduledTaskAdminConfig.getAdminConfig().getTriggerPoolSlowMax(),
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(2000),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "scheduled_task, admin JobTriggerPoolHelper-slowTriggerPool-" + r.hashCode());
                    }
                }
        );

    }

    /**
     * 关闭两个线程池
     */
    public void stop() {
        fastTriggerPool.shutdownNow();
        slowTriggerPool.shutdownNow();
        logger.info(">>>>>>>>> scheduled_task trigger thread pool shutdown success.");
    }


    /**
     * 分钟数，用于刷新任务的慢执行次数
     */
    private volatile long minTime = System.currentTimeMillis() / 60000;

    /**
     * 记录每分钟内任务的慢执行次数
     */
    private volatile ConcurrentHashMap<Integer, AtomicInteger> jobTimeoutCountMap = new ConcurrentHashMap<>();

    /**
     * 对外暴露的调度方法
     *
     * @param jobId                 任务id
     * @param triggerType           调度类型，默认cron
     * @param failRetryCount        失败重试次数
     * @param executorShardingParam 分片参数
     * @param executorParam         参数
     * @param addressList           执行器集合
     */
    public static void trigger(int jobId,
                               TriggerTypeEnum triggerType,
                               int failRetryCount,
                               String executorShardingParam,
                               String executorParam,
                               String addressList) {
        jobTriggerPoolHelper.addTrigger(jobId, triggerType, failRetryCount, executorShardingParam, executorParam, addressList);

    }

    /**
     * 内部实际完成功能的方法，将调度任务区分快慢后交给线程池，线程池将任务提交给 ScheduledTaskTrigger，让它去发送调度信息
     *
     * @param jobId                 任务id
     * @param triggerType           调度类型，默认cron
     * @param failRetryCount        失败重试次数
     * @param executorShardingParam 分片参数
     * @param executorParam         参数
     * @param addressList           执行器集合
     */
    private void addTrigger(int jobId,
                            TriggerTypeEnum triggerType,
                            int failRetryCount,
                            String executorShardingParam,
                            String executorParam,
                            String addressList) {
        // 选择快/慢线程池
        ThreadPoolExecutor triggerPool = slowTriggerPool;
        AtomicInteger timeoutCount = jobTimeoutCountMap.get(jobId);
        if (timeoutCount != null && timeoutCount.get() >= 10) {
            triggerPool = fastTriggerPool;
        }
        // 交给线程池调度, 调度后统计此次任务消耗时间，判断是否为慢执行
        triggerPool.execute(() -> {
            long start = System.currentTimeMillis();
            try {
                ScheduledTaskTrigger.trigger(jobId, triggerType, failRetryCount, executorShardingParam, executorParam, addressList);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                // 判断是否是同一分钟内
                long mintTime_now = System.currentTimeMillis() / 60000;
                if (minTime != mintTime_now) {
                    jobTimeoutCountMap.clear();
                    minTime = mintTime_now;
                }
                // 如果耗时超过500ms，记录为一次慢执行
                long end = System.currentTimeMillis();
                if (end - start > 500) {
                    AtomicInteger atomicInteger = jobTimeoutCountMap.putIfAbsent(jobId, new AtomicInteger(1));
                    if (atomicInteger != null) {
                        atomicInteger.incrementAndGet();
                    }

                }
            }

        });


    }


}
