package com.xiaohe.core.thread;

import com.xiaohe.core.conf.ScheduledTaskAdminConfig;
import com.xiaohe.core.cron.CronExpression;
import com.xiaohe.core.model.ScheduledTaskInfo;
import com.xiaohe.core.scheduler.MisfireStrategyEnum;
import com.xiaohe.core.scheduler.ScheduleTypeEnum;
import com.xiaohe.core.trigger.TriggerTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author : 小何
 * @Description : 从数据库中查出将要执行的任务，放入时间轮中，调用JobTriggerPoolHelper去发送任务执行的消息
 * @date : 2023-09-14 23:22
 */
public class JobScheduleHelper {
    private static Logger logger = LoggerFactory.getLogger(JobScheduleHelper.class);

    private static JobScheduleHelper instance = new JobScheduleHelper();

    public static JobScheduleHelper getInstance() {
        return instance;
    }

    /**
     * 预读时间，从数据库中读出 5s 后需要执行的任务。
     */
    public static final long PRE_READ_MS = 5000;

    /**
     * 从数据库中读取任务放入时间轮的线程
     */
    private Thread scheduleThread;

    /**
     * 从世间轮中取出任务，交给快慢线程池调度的线程
     */
    private Thread ringThread;

    /**
     * 调度线程是否停止工作
     */
    private volatile boolean scheduleThreadToStop = false;

    /**
     * 时间轮线程池是否停止工作
     */
    private volatile boolean ringThreadToStop = false;

    /**
     * 时间轮容器         <br></br>
     * key : 一分钟之内的秒数，0-59      <br></br>
     * value: 这一秒内所有需要执行的定时任务id
     */
    private volatile static ConcurrentHashMap<Integer, List<Integer>> ringData = new ConcurrentHashMap<>();

    /**
     * 根据用户选择的调度类型(cron、固定频率) 刷新定时任务的下一次执行时间
     *
     * @param jobInfo
     * @param fromTime
     */
    private void refreshNextValidTime(ScheduledTaskInfo jobInfo, Date fromTime) throws ParseException {
        Date nextValidTime = null;
        // 根据用户选择的调度类型(cron、固定频率) 确定下次执行时间
        ScheduleTypeEnum scheduleType = ScheduleTypeEnum.match(jobInfo.getScheduleType(), null);
        if (ScheduleTypeEnum.CRON.equals(scheduleType)) {
            nextValidTime = new CronExpression(jobInfo.getScheduleConf()).getNextValidTimeAfter(fromTime);
        } else if (ScheduleTypeEnum.FIX_RATE.equals(scheduleType)) {
            nextValidTime = new Date(fromTime.getTime() + Integer.parseInt(jobInfo.getScheduleConf()) * 1000L);
        }
        // 如果下次执行时间不为空，说明用户想要调度
        if (nextValidTime != null) {
            jobInfo.setTriggerLastTime(jobInfo.getTriggerNextTime());
            jobInfo.setTriggerNextTime(nextValidTime.getTime());
        } else {
            // 如果为空，说明用户不想调度，将任务的调度状态改为0
            jobInfo.setTriggerStatus(0);
            jobInfo.setTriggerNextTime(0);
            jobInfo.setTriggerLastTime(0);
        }
    }

    /**
     * 将任务放入时间轮中
     *
     * @param ringSecond 刻度
     * @param jobId      任务id
     */
    private void pushTimeRing(int ringSecond, int jobId) {
        List<Integer> ringItemData = ringData.get(ringSecond);
        if (ringItemData == null) {
            ringItemData = new ArrayList<>();
            ringData.put(ringSecond, ringItemData);
        }
        ringItemData.add(jobId);
        logger.debug(">>>>>>>>>>>>  schedule push time-ring: " + ringSecond + " = " + Arrays.asList(ringItemData));
    }

    private void process(ScheduledTaskInfo jobInfo, long nowTime) throws ParseException {
        // 此任务本应该在 46s 执行。现在41s，正好扫描出来此任务并将其放入时间轮中，但是此时恰好宕机。
        // 1. 一直到 44s 恢复，扫描出这个将要执行的任务， 直接将其放入时间轮中正常执行就行。
        // 2. 一直到 48s 恢复，使用 trigger_next_time < (48+5) 将这个本应该在 46s 执行的任务扫了出来，立刻做补救措施，`当即执行一次`，计算并刷新下次执行时间，如果下次执行仍在5s调度周期中就将它`放入时间轮中`，如果没有在5s周期内，那就等以后的循环再执行吧
        // 3. 一直到 55s 恢复，这个任务可能2s执行一次，所以可能已经错过很多次了，这时候就看用户设定的策略，
        //    有摆烂不管，有立即执行一次。
        if (nowTime > jobInfo.getTriggerNextTime() + PRE_READ_MS) {
            // 默认啥也不干，但是如果失败策略是立即执行一次，那就执行呗
            logger.warn(">>>>>>>>>>>>>>>> schedule misfire, jobId = " + jobInfo.getId());
            MisfireStrategyEnum misfireStrategy = MisfireStrategyEnum.match(jobInfo.getMisfireStrategy(), MisfireStrategyEnum.DO_NOTHING);
            if (MisfireStrategyEnum.FIRE_ONCE_NOW.equals(misfireStrategy)) {
                JobTriggerPoolHelper.trigger(jobInfo.getId(), TriggerTypeEnum.MISFIRE, -1, null, null, null);
                logger.debug(">>>>>>>>>>>>>>>>> schedule push trigger: jobId = " + jobInfo.getId());
            }
            refreshNextValidTime(jobInfo, new Date());
        } else if (nowTime > jobInfo.getTriggerNextTime()) {
            // 立即执行一次
            JobTriggerPoolHelper.trigger(jobInfo.getId(), TriggerTypeEnum.CRON, -1, null, null, null);
            refreshNextValidTime(jobInfo, new Date());
            logger.debug(">>>>>>>>>>>>>>>>> schedule push trigger: jobId = " + jobInfo.getId());
            // 如果该任务的下次执行时间在5s内，现在就将它放在时间轮中，不用等下次循环了
            if (jobInfo.getTriggerStatus() == 1 && nowTime + PRE_READ_MS > jobInfo.getTriggerNextTime()) {
                int ringSecond = (int) (jobInfo.getTriggerNextTime() / 1000 % 60);
                pushTimeRing(ringSecond, jobInfo.getId());
                refreshNextValidTime(jobInfo, new Date(jobInfo.getTriggerNextTime()));
            }
        } else {
            int ringSecond = (int) (jobInfo.getTriggerNextTime() / 1000 % 60);
            pushTimeRing(ringSecond, jobInfo.getId());
            refreshNextValidTime(jobInfo, new Date(jobInfo.getTriggerNextTime()));
        }

    }

    public void start() {
        scheduleThread = new Thread(() -> {
            // 线程刚刚启动，保证此线程开始工作的时间恰好是整秒，以后好计算
            try {
                TimeUnit.MILLISECONDS.sleep(5000 - System.currentTimeMillis() % 1000);
            } catch (InterruptedException e) {
                if (!scheduleThreadToStop) {
                    logger.error(e.getMessage(), e);
                }
            }
            // 每秒最大执行数
            // 计算规则: 假设每秒能执行20个任务(xxl认为一个任务执行时间为50ms)，快慢线程池共有300个线程，那么最多一秒执行 6000 个任务
            // 但是快慢线程池的最大线程数可以更改，所以要从 ScheduledTaskAdminConfig 中读取
            int preReadCount = (ScheduledTaskAdminConfig.getAdminConfig().getTriggerPoolFastMax()
                    + ScheduledTaskAdminConfig.getAdminConfig().getTriggerPoolSlowMax()) * 20;

            // 开始读取数据库中的定时任务
            while (!scheduleThreadToStop) {
                // 这个时间用于判断此次循环耗时
                long start = System.currentTimeMillis();
                // 调度中心抢分布式锁，哪个调度中心抢到，它就有指挥执行器的权力，分布式锁肯定不能自动提交事务
                Connection conn = null;
                Boolean connAutoCommit = false;
                PreparedStatement preparedStatement = null;
                // 数据库中是否有数据
                boolean preReadSuc = true;
                try {
                    conn = ScheduledTaskAdminConfig.getAdminConfig().getDataSource().getConnection();
                    connAutoCommit = conn.getAutoCommit();
                    conn.setAutoCommit(false);
                    preparedStatement = conn.prepareStatement("select * from scheduled_task_lock where lock_name = 'schedule_lock' for update");
                    preparedStatement.execute();
                    // 这个时间用于从数据库中获取任务
                    long nowTime = System.currentTimeMillis();
                    List<ScheduledTaskInfo> scheduleList = ScheduledTaskAdminConfig.getAdminConfig().getScheduledTaskInfoMapper().scheduleJobQuery(nowTime + PRE_READ_MS, preReadCount);
                    if (CollectionUtils.isEmpty(scheduleList)) {
                        preReadSuc = false;
                        continue;
                    }
                    for (ScheduledTaskInfo jobInfo : scheduleList) {
                        //
                        process(jobInfo, nowTime);
                    }
                    // 将刚刚定时任务刷新到数据库中
                    ScheduledTaskAdminConfig.getAdminConfig().getScheduledTaskInfoMapper().scheduleUpdate(scheduleList);

                } catch (SQLException | ParseException e) {
                    if (!scheduleThreadToStop) {
                        logger.error(e.getMessage(), e);
                    }
                } finally {
                    // 提交事务，释放锁，设置非手动提交
                    if (conn != null) {
                        try {
                            conn.commit();
                        } catch (SQLException e) {
                            logger.error(e.getMessage(), e);
                        }

                        try {
                            conn.setAutoCommit(connAutoCommit);
                        } catch (SQLException e) {
                            logger.error(e.getMessage(), e);
                        }

                        try {
                            conn.close();
                        } catch (SQLException e) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                    if (null != preparedStatement) {
                        try {
                            preparedStatement.close();
                        } catch (SQLException e) {
                            logger.error(e.getMessage(), e);
                        }
                    }

                }
                // 此次从数据库中查出的任务处理了多长时间
                long cost = System.currentTimeMillis() - start;
                if (cost < 1000) {
                    try {
                        // 如果没扫描出数据，说明未来5s内没有任务要执行，睡5s
                        // 如果扫描出了任务，对齐整秒
                        TimeUnit.MILLISECONDS.sleep((preReadSuc ? 1000 : PRE_READ_MS) - System.currentTimeMillis() % 1000);
                    } catch (InterruptedException e) {
                        logger.error(e.getMessage(), e);
                    }
                }

            }
            // 退出while循环，线程需要终止
            logger.info(">>>>>>>>>>> xxl-job, JobScheduleHelper#scheduleThread stop");
        });
        scheduleThread.setDaemon(true);
        scheduleThread.setName("xxl-job, admin JobScheduleHelper#scheduleThread");
        scheduleThread.start();

        // 时间轮工作线程, 从时间轮中取出任务执行
        ringThread = new Thread(() -> {
            while (!ringThreadToStop) {
                // 每1s调度一次
                try {
                    TimeUnit.MILLISECONDS.sleep(1000 - System.currentTimeMillis() % 1000);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }
                //
                List<Integer> ringItemData = new ArrayList<>();
                int second = (int) System.currentTimeMillis() / 1000 % 60;
                // 为了防止某个刻度中的任务太多，每一次调度都调度 second和 second-1 这两个刻度上的任务
                for (int i = 0; i < 2; i++) {
                    List<Integer> tempData = ringData.get((second + 60 - i) % 60);
                    ringItemData.addAll(tempData);
                }
                logger.debug("time-ring beat, second: " + second + ": " + ringItemData);
                for (Integer jobId : ringItemData) {
                    JobTriggerPoolHelper.trigger(jobId, TriggerTypeEnum.CRON, -1, null, null, null);
                }
                ringItemData.clear();
            }

        });
        ringThread.setDaemon(true);
        ringThread.setName("xxl-job, admin JobScheduleHelper#ringThread");
        ringThread.start();
    }


    /**
     * 停止 从数据库中扫描任务放入时间轮的线程 和 从时间轮中取出任务放入快慢线程池中的线程
     */
    public void toStop() {
        // 停止 scheduleThread，(停止从数据库中取出任务)
        scheduleThreadToStop = true;
        if (scheduleThread.getState() != Thread.State.TERMINATED) {
            scheduleThread.interrupt();
            try {
                scheduleThread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }

        // 看看时间轮中还有没有数据，如果有，给它8s执行
        boolean hasRingData = false;
        if (!CollectionUtils.isEmpty(ringData)) {
            for (Integer second : ringData.keySet()) {
                List<Integer> tmpData = ringData.get(second);
                if (!CollectionUtils.isEmpty(tmpData)) {
                    hasRingData = true;
                    break;
                }
            }
        }
        if (hasRingData) {
            try {
                TimeUnit.SECONDS.sleep(8);
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
        }
        // 有数据就执行8s，没数据就立马停止这个线程
        ringThreadToStop = true;
        if (ringThread.getState() != Thread.State.TERMINATED){
            ringThread.interrupt();
            try {
                ringThread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
        logger.info(">>>>>>>>>>> xxl-job, JobScheduleHelper stop");
    }


}
