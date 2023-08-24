package com.xiaohe.ScheduledTask.admin.core.thread;

import com.xiaohe.ScheduledTask.admin.core.model.ScheduledTaskInfo;
import com.xiaohe.ScheduledTask.admin.core.trigger.ScheduledTaskTrigger;
import com.xiaohe.ScheduledTask.core.util.ObjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-08-23 14:17
 */
public class JobTriggerPoolHelper {

    private static Logger logger = LoggerFactory.getLogger(JobTriggerPoolHelper.class);
    private static final JobTriggerPoolHelper jobTriggerPoolHelper = new JobTriggerPoolHelper();

    public static JobTriggerPoolHelper getInstance() {
        return jobTriggerPoolHelper;
    }

    /**
     * 快线程池，默认使用
     */
    private ThreadPoolExecutor fastTriggerPool = null;
    /**
     * 慢线程池
     */
    private ThreadPoolExecutor slowTriggerPool = null;

    /**
     * 对外暴露的start方法，调用后创建线程池
     */
    public static void toStart() {
        jobTriggerPoolHelper.start();
    }

    /**
     * 创建线程池
     */
    private void start() {
        // 快线程池，最大线程数为200
        fastTriggerPool = new ThreadPoolExecutor(
                10,
                200,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(1000),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "ScheduledTask, admin JobTriggerPoolHelper-fastTriggerPool-" + r.hashCode());
                    }
                });
        //慢线程池，最大线程数为100
        slowTriggerPool = new ThreadPoolExecutor(
                10,
                100,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(2000),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "ScheduledTask, admin JobTriggerPoolHelper-slowTriggerPool-" + r.hashCode());
                    }
                });
    }

    /**
     * 对外暴露的停止方法，调用后销毁线程池
     */
    public static void toStop() {
        jobTriggerPoolHelper.stop();
    }

    private void stop() {
        fastTriggerPool.shutdown();
        slowTriggerPool.shutdown();
    }


    /**
     * 对外暴露的提交任务的接口
     *
     * @param jobInfo
     */
    public static void trigger(ScheduledTaskInfo jobInfo) {
        jobTriggerPoolHelper.addTrigger(jobInfo);
    }

    /**
     * 系统当前分钟，每过一分钟刷新Map，记录一分钟内慢执行任务的次数
     */
    private volatile long minTime = System.currentTimeMillis() / 60000;

    /**
     * 记录慢执行任务的Map
     * key: 任务id
     * value: 该任务慢执行次数
     */
    private volatile ConcurrentHashMap<Integer, AtomicInteger> jobTimeoutCountMap = new ConcurrentHashMap<>();

    /**
     * 内部使用的真正的提交任务的方法, 仅仅是提交，并没有执行，需要调用 ScheduledTaskTrigger.trigger 去执行
     *
     * @param jobInfo
     */
    private void addTrigger(ScheduledTaskInfo jobInfo) {
        // 用线程池提交任务, 默认使用快线程池
        ThreadPoolExecutor triggerPool = fastTriggerPool;

        // 得到慢执行次数如果一分钟内的慢执行次数大于等于10，将任务交给慢线程池处理
        AtomicInteger jobTimeoutCount = jobTimeoutCountMap.get(jobInfo.getId());
        if (ObjectUtil.isNotNull(jobTimeoutCount) && jobTimeoutCount.get() >= 10) {
            triggerPool = slowTriggerPool;
        }

        triggerPool.execute(() -> {
            // 记录当前时间，用于计算任务消耗时间
            long now = System.currentTimeMillis();
            try {
                ScheduledTaskTrigger.trigger(jobInfo);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                // 先判断Map是否需要刷新
                long minTimeNow = System.currentTimeMillis() / 60000;
                if (minTimeNow != minTime) {
                    minTime = minTimeNow;
                    jobTimeoutCountMap.clear();
                }
                // 计算任务消耗时间
                long cost = System.currentTimeMillis() - now;
                // 超过500ms记录为一次慢执行
                if (cost > 500) {
                    // 如果为空就添加，如果不为空就加一
                    AtomicInteger timeoutCount = jobTimeoutCountMap.putIfAbsent(jobInfo.getId(), new AtomicInteger(1));
                    if (ObjectUtil.isNotNull(timeoutCount)) {
                        timeoutCount.addAndGet(1);
                    }
                }
            }
        });

    }
}
