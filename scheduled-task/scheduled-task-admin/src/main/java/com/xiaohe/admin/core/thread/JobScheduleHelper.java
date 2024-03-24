package com.xiaohe.admin.core.thread;

import com.xiaohe.admin.core.conf.ScheduleTaskAdminConfig;
import com.xiaohe.admin.core.cron.CronExpression;
import com.xiaohe.admin.core.model.XxlJobInfo;
import com.xiaohe.admin.core.scheduler.MisfireStrategyEnum;
import com.xiaohe.admin.core.scheduler.ScheduleTypeEnum;
import com.xiaohe.admin.core.trigger.TriggerTypeEnum;
import com.xiaohe.core.util.CollectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author : 小何
 * @Description : 从数据库中查出5秒内将要执行的任务。交给 JobTriggerPool进行调度。
 * @date : 2023-10-18 20:08
 */
public class JobScheduleHelper {
    private static Logger logger = LoggerFactory.getLogger(JobScheduleHelper.class);
    private static JobScheduleHelper instance = new JobScheduleHelper();

    public static JobScheduleHelper getInstance() {
        return instance;
    }

    /**
     * 预读时间
     */
    public static final long PRE_READ_MS = 5000;

    /**
     * 从数据库中查询任务放入时间轮中
     */
    private Thread scheduleThread;

    /**
     * 从时间轮中取任务交给 JobTriggerPoolHelper 调度
     */
    private Thread ringThread;

    /**
     * 数据库查询线程是否停止
     */
    private volatile boolean scheduleThreadToStop = false;

    /**
     * 时间轮线程是否停止
     */
    private volatile boolean ringThreadToStop = false;

    /**
     * 时间轮
     * key : 时间下标，数字代表一分钟内的时间。比如 5 代表这分钟内的第五秒
     * value : 这个时间内需要执行的任务
     */
    private volatile static Map<Integer, List<Integer>> ringData = new ConcurrentHashMap<>();

    public void start() {
        initScheduleThread();
        initRingThread();
        logger.info(">>>>>>>>>>>> Scheduled Task, JobScheduleHelper start success");
    }


    private void initScheduleThread() {
        scheduleThread = new Thread(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(5000 - System.currentTimeMillis() % 1000);
            } catch (InterruptedException e) {
                if (!scheduleThreadToStop) {
                    logger.error(e.getMessage(), e);
                }
            }
            logger.info(">>>>>>>>>> init Scheduled Task scheduler success.");

            // 默认预读数量，根据配置的快慢线程池数量来决定一次性(1s)能处理多少任务。默认一个任务50ms，那么1s内能执行20个任务。
            int preReadCount = (ScheduleTaskAdminConfig.getAdminConfig().getTriggerPoolFastMax() + ScheduleTaskAdminConfig.getAdminConfig().getTriggerPoolSlowMax()) % (1000 / 50);
            while (!scheduleThreadToStop) {
                // start记录扫描数据库花了多长时间
                long start = System.currentTimeMillis();
                Connection conn = null;
                Boolean connAutoCommit = null;
                PreparedStatement preparedStatement = null;
                boolean preReadSuc = true;
                try {
                    conn = ScheduleTaskAdminConfig.getAdminConfig().getDataSource().getConnection();
                    connAutoCommit = conn.getAutoCommit();
                    // 事务
                    conn.setAutoCommit(false);
                    preparedStatement = conn.prepareStatement("select * from xxl_job_lock where lock_name = 'schedule_lock' for update");
                    // 执行，得到数据库锁
                    preparedStatement.execute();
                    // now 用于从数据库扫出来任务 now + 5000
                    long now = System.currentTimeMillis();
                    List<XxlJobInfo> xxlJobInfos = ScheduleTaskAdminConfig.getAdminConfig().getXxlJobInfoMapper().scheduleJobQuery(now + PRE_READ_MS, preReadCount);
                    if (CollectionUtil.isEmpty(xxlJobInfos)) {
                        preReadSuc = false;
                        continue;
                    }
                    for (XxlJobInfo jobInfo : xxlJobInfos) {
                        // 一个任务从数据库中被查出来的区间是5s，假如某任务第11s执行，那么照理来说能让它执行的时间区间为 [6, 11]。
                        // 但是如果现在到了12s，有可能是任务实在太多、MySQL的IO操作太耗时了调度不过来，那么将任务执行的5s后为补偿时间，此处为 [12, 16]。也就是你错过了但是我补偿你重新调度一次。
                        // 而且如果下次执行在5s的区间，就顺便将任务放入时间轮中
                        if (jobInfo.getTriggerNextTime() + PRE_READ_MS > now && now > jobInfo.getTriggerNextTime()) {
                            logger.warn(">>>>>>>>>>> Scheduled Task, schedule misfire, jobId: " + jobInfo.getId());
                            // 调度
                            JobTriggerPoolHelper.trigger(jobInfo.getId(), TriggerTypeEnum.CRON, -1, null, null, null);
                            logger.debug(">>>>>>>>>> Scheduled Task, schedule push trigger : jobId = " + jobInfo.getId());
                            // 刷新下次执行时间
                            refreshNextValidTime(jobInfo, new Date());
                            if (jobInfo.getTriggerStatus() == 1 && now + PRE_READ_MS > jobInfo.getTriggerNextTime()) {
                                int ringSecond = (int) ((jobInfo.getTriggerNextTime() / 1000) % 60);
                                pushTimeRing(ringSecond, jobInfo.getId());
                                refreshNextValidTime(jobInfo, new Date(jobInfo.getTriggerNextTime()));
                            }
                        } else if (jobInfo.getTriggerNextTime() + PRE_READ_MS < now) {
                            // 这个任务需要在11s执行，如果现在的时间在 [11, 16] 之间，还可以弥补，但是现在的时间在 19s，已经远远超过它应该调度的时间。这种情况大概率是调度中心宕机了。
                            // 也就是这个任务根本没有被调度导致现在已经错过5s了。怎么办？使用调度过期策略
                            logger.debug(">>>>>>>>>>>> Scheduled Task, schedule misfire, jobId = " + jobInfo.getId());
                            MisfireStrategyEnum misfireStrategy = MisfireStrategyEnum.match(jobInfo.getMisfireStrategy(), null);
                            if (misfireStrategy == MisfireStrategyEnum.FIRE_ONCE_NOW) {
                                JobTriggerPoolHelper.trigger(jobInfo.getId(), TriggerTypeEnum.MISFIRE, -1, null, null, null);
                                logger.debug(">>>>>>>>>>> Scheduled Task, schedule push trigger : jobId = " + jobInfo.getId());
                            }
                            refreshNextValidTime(jobInfo, new Date());
                        } else {
                            // 这是最正常的情况，任务需要在11s执行，现在的时间走到了9s。那就将这个任务放到时间轮中。
                            int ringSecond = (int) (jobInfo.getTriggerNextTime() / 1000 % 60);
                            pushTimeRing(ringSecond, jobInfo.getId());
                            refreshNextValidTime(jobInfo, new Date(jobInfo.getTriggerNextTime()));
                        }

                    }
                    // 更新一下上面任务的状态。
                    ScheduleTaskAdminConfig.getAdminConfig().getXxlJobInfoMapper().scheduleUpdate(xxlJobInfos);
                } catch (Exception e) {
                    if (!scheduleThreadToStop) {
                        logger.error(">>>>>>>>>>> Scheduled Task, JobScheduleHelper#scheduleThread error:{}", e);
                    }
                } finally {
                    // 提交事务、关闭资源
                    closeResource(conn, connAutoCommit, preparedStatement);
                }
                // 记录上述查询消耗了多少时间, 如果小于1s，那么数据库里面可能没啥数据.
                // 如果真的一个数据都没有，那就睡5s. (要进行时间对齐)
                // 如果有数据，说明少，那就睡1s. (要进行时间对齐)
                long cost = System.currentTimeMillis() - start;
                if (cost < 1000) {
                    try {
                        TimeUnit.MILLISECONDS.sleep((preReadSuc ? 1000 : PRE_READ_MS) - System.currentTimeMillis() % 1000);
                    } catch (InterruptedException e) {
                        if (!scheduleThreadToStop) {
                            logger.error(e.getMessage());
                        }
                    }

                }
            }
            logger.info(">>>>>>>>>>> Scheduled Task, JobScheduleHelper#scheduleThread stop");
        });
        //设置守护线程，启动线程
        scheduleThread.setDaemon(true);
        scheduleThread.setName("Scheduled Task, admin JobScheduleHelper#scheduleThread");
        scheduleThread.start();
    }

    private void closeResource(Connection conn, Boolean connAutoCommit, PreparedStatement preparedStatement) {
        if (conn != null) {
            try {
                conn.commit();
            } catch (SQLException e) {
                if (!scheduleThreadToStop) {
                    logger.error(e.getMessage());
                }
            }
            try {
                conn.setAutoCommit(connAutoCommit);
            } catch (SQLException e) {
                if (!scheduleThreadToStop) {
                    logger.error(e.getMessage());
                }
            }
            try {
                conn.close();
            } catch (SQLException e) {
                if (!scheduleThreadToStop) {
                    logger.error(e.getMessage());
                }
            }
        }
        if (preparedStatement != null) {
            try {
                preparedStatement.close();
            } catch (SQLException e) {
                if (!scheduleThreadToStop) {
                    logger.error(e.getMessage());
                }
            }
        }
    }

    private void initRingThread() {
        ringThread = new Thread(() -> {
            while (!ringThreadToStop) {
                // 时间对齐，保证每一次循环都是这一秒的开始
                try {
                    TimeUnit.MILLISECONDS.sleep(1000 - System.currentTimeMillis() % 1000);
                } catch (Exception e) {
                    if (!ringThreadToStop) {
                        logger.error(e.getMessage(), e);
                    }
                }
                try {
                    List<Integer> ringItemData = new ArrayList<>();
                    int nowSecond = Calendar.getInstance().get(Calendar.SECOND);
                    for (int i = 0; i < 2; i++) {
                        List<Integer> tmpData = ringData.remove((nowSecond + 60 - i) % 60);
                        if (tmpData != null) {
                            ringItemData.addAll(tmpData);
                        }
                    }
                    logger.debug(">>>>>>>>>>>> Scheduled Task, time-ring beat : " + nowSecond + " = " + ringItemData);
                    for (int jobId : ringItemData) {
                        JobTriggerPoolHelper.trigger(jobId, TriggerTypeEnum.CRON, -1, null, null, null);
                    }
                    ringItemData.clear();
                } catch (Exception e) {
                    if (!ringThreadToStop) {
                        logger.error(e.getMessage());
                    }
                }
            }
            logger.info(">>>>>>>>>>> Scheduled Task, JobScheduleHelper#ringThread stop");
        });
        ringThread.setDaemon(true);
        ringThread.setName("Scheduled Task, admin JobScheduleHelper#ringThread");
        ringThread.start();

    }

    private void pushTimeRing(int ringSecond, int jobId) {
        // 得到对应秒内将要执行的任务
        List<Integer> ringItemData = ringData.get(ringSecond);
        if (ringItemData == null) {
            ringItemData = new ArrayList<>();
            ringData.put(ringSecond, ringItemData);
        }
        ringItemData.add(jobId);
        logger.debug(">>>>>>>>>>>> Scheduled Task, schedule push time-ring : " + ringSecond + " = " + ringItemData);
    }

    private void refreshNextValidTime(XxlJobInfo jobInfo, Date fromTime) throws ParseException {
        Date nextValidTime = generateNextValidTime(jobInfo, fromTime);
        if (nextValidTime != null) {
            jobInfo.setTriggerLastTime(jobInfo.getTriggerNextTime());
            jobInfo.setTriggerNextTime(nextValidTime.getTime());
        } else {
            jobInfo.setTriggerStatus(0);
            jobInfo.setTriggerLastTime(0);
            logger.warn(">>>>>>>>>>> Scheduled Task, refreshNextValidTime fail for job: {}, scheduleType: {}, scheduleConf: {}", jobInfo.getId(), jobInfo.getScheduleType(), jobInfo.getScheduleConf());
        }
    }

    /**
     * 根据调度策略的不同生成调度时间，比如corn、固定频率
     *
     * @param jobInfo
     * @param fromTime
     */
    public static Date generateNextValidTime(XxlJobInfo jobInfo, Date fromTime) throws ParseException {
        ScheduleTypeEnum scheduleType = ScheduleTypeEnum.match(jobInfo.getScheduleType(), null);
        if (scheduleType == ScheduleTypeEnum.CRON) {
            Date nextValidTimeAfter = new CronExpression(jobInfo.getScheduleConf()).getNextValidTimeAfter(fromTime);
            return nextValidTimeAfter;
        } else if (scheduleType == ScheduleTypeEnum.FIX_RATE) {
            return new Date(fromTime.getTime() + Integer.valueOf(jobInfo.getScheduleConf()) * 1000);
        }
        return null;
    }

    public void toStop() {
        scheduleThreadToStop = true;
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        if (scheduleThread.getState() != Thread.State.TERMINATED) {
            scheduleThread.interrupt();
            try {
                scheduleThread.join();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        boolean hasRingData = false;
        for (Integer second : ringData.keySet()) {
            List<Integer> tmpData = ringData.get(second);
            if(!CollectionUtil.isEmpty(tmpData)) {
                hasRingData = true;
                break;
            }
        }
        if (hasRingData) {
            try {
                TimeUnit.SECONDS.sleep(8);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
        ringThreadToStop = true;
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
        if (ringThread.getState() != Thread.State.TERMINATED) {
            ringThread.interrupt();
            try {
                ringThread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
        }
        logger.info(">>>>>>>>>>> Scheduled Taskb, JobScheduleHelper stop");
    }
}
