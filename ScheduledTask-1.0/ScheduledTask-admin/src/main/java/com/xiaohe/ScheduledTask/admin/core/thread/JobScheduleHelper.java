package com.xiaohe.ScheduledTask.admin.core.thread;

import com.xiaohe.ScheduledTask.admin.core.cron.CronExpression;
import com.xiaohe.ScheduledTask.admin.core.model.ScheduledTaskInfo;
import com.xiaohe.ScheduledTask.core.util.CollectionUtil;
import com.xiaohe.ScheduledTask.core.util.ObjectUtil;
import com.xiaohe.ScheduledTask.admin.dao.ScheduledTaskInfoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author : 小何
 * @Description : 此类用于从数据库中查询将要执行的定时任务，并将它交给 JobTriggerPoolHelper 去调度
 * @date : 2023-08-23 11:52
 */
public class JobScheduleHelper {
    private static Logger logger = LoggerFactory.getLogger(JobScheduleHelper.class);
    /**
     * 单例模式
     */
    private static JobScheduleHelper jobScheduleHelper = new JobScheduleHelper();

    public static JobScheduleHelper getInstance() {
        return jobScheduleHelper;
    }


    /**
     * 查询指定时间内需要执行的定时任务，并将它放入时间轮。
     */
    private Thread scheduleThread;

    /**
     * 从时间轮中取出定时任务，交给线程池去调度
     */
    private Thread ringThread;


    private ScheduledTaskInfoMapper scheduledTaskInfoMapper;

    /**
     * 时间轮容器，一共有60个刻度 = 60s，每一个刻度存放这一秒内需要执行的所有任务id，
     * key: Integer, 刻度，s
     * value: List<Integer> 此刻度对应的所有定时任务id
     */
    private volatile static Map<Integer, List<Integer>> ringData = new ConcurrentHashMap<>();

    /**
     * 查询5s内可以执行的所有定时任务
     */
    public static final long PRE_READ_MS = 5000;

    /**
     * 启动 scheduleThread
     */
    public void start() {
        scheduleThread = new Thread(() -> {
            while (true) {
                // 本次扫描是否扫描到了数据
                boolean preReadSuc = true;
                // 开始扫描数据库
                long nowTime = System.currentTimeMillis();
                // 读出未来5s将要执行的定时任务
                List<ScheduledTaskInfo> jobInfoList = scheduledTaskInfoMapper.scheduleJobQuery(nowTime + PRE_READ_MS, 6000);
                if (!CollectionUtil.isEmpty(jobInfoList)) {
                    for (ScheduledTaskInfo jobInfo : jobInfoList) {
                        // 如果当前时间 大于 该任务的下一次执行时间+5s
                        // 比如 当前时间为12s，任务的执行时间为5s, 但是机器在第4s宕机了。即12 > 5 + 5
                        if (nowTime > jobInfo.getTriggerNextTime() + PRE_READ_MS) {
                            // 立即补发一次，不放入时间轮，直接让线程池执行
                            JobTriggerPoolHelper.trigger(jobInfo);
                            // 刷新时间
                            refreshNextValidTime(jobInfo, new Date());
                        } else if (nowTime > jobInfo.getTriggerNextTime()) {
                            // 虽然当前时间超过了该任务的下次执行时间，但没有超过5s的周期
                            JobTriggerPoolHelper.trigger(jobInfo);
                            refreshNextValidTime(jobInfo, new Date());
                            // 如果此任务的下一次执行时间在本次调度周期内, 将它放到
                            if (nowTime + PRE_READ_MS > jobInfo.getTriggerNextTime()) {
                                int ringSecond = (int) ((jobInfo.getTriggerNextTime() / 1000) % 60);
                                pushTimeRing(ringSecond, jobInfo.getId());
                                refreshNextValidTime(jobInfo, new Date(jobInfo.getTriggerNextTime()));
                            }
                        } else {
                            // 最正常的任务
                            int ringSecond = (int) ((jobInfo.getTriggerNextTime() / 1000) % 60);
                            pushTimeRing(ringSecond, jobInfo.getId());
                            refreshNextValidTime(jobInfo, new Date(jobInfo.getTriggerNextTime()));
                        }

                    }
                    // 更新所有定时任务的信息
                    for (ScheduledTaskInfo jobInfo : jobInfoList) {
                        scheduledTaskInfoMapper.scheduleUpdate(jobInfo);
                    }


                } else {
                    // 没有定时任务在5s内执行
                    preReadSuc = false;
                }

                // 计算扫描数据库并将所有任务放入时间轮花费的时间
                long cost = System.currentTimeMillis() - nowTime;
                // 如果耗时小于1s，说明数据库里面接下来5s要执行的没多少任务, 让线程睡觉。
                // 如果根本没扫描出来任务，睡5s
                // 如果有但少，睡到下一秒再工作
                if (cost < 1000) {
                    try {
                        TimeUnit.MILLISECONDS.sleep((preReadSuc ? 1000 : PRE_READ_MS) - System.currentTimeMillis() % 1000);
                    } catch (InterruptedException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        });
        scheduleThread.start();

        ringThread = new Thread(() -> {
            while (!ringThreadToStop) {
                // 让该线程每1s调度一次，如果不足1s，就让它睡1s, 反正让它在每一秒的开始再工作
                // 避免出现: 此轮刻度在0.3s时调度完成，下一轮立即开始
                try {
                    TimeUnit.MILLISECONDS.sleep(1000 - System.currentTimeMillis() % 1000);
                } catch (InterruptedException e) {
                    if (!ringThreadToStop) {
                        logger.error(e.getMessage(), e);
                    }
                }
                // 开始将时间轮刻度上的任务提交处理，并删除
                try {
                    ArrayList<Integer> ringItemData = new ArrayList<>();
                    int nowSecond = Calendar.getInstance().get(Calendar.SECOND);
                    // 将此秒和上一秒未能调度的任务都取出来(如果有的话)
                    for (int i = 0; i < 2; i++) {
                        List<Integer> tmpData = ringData.remove((nowSecond + 60 - i) % 60);
                        if (ObjectUtil.isNotNull(tmpData)) {
                            ringItemData.addAll(tmpData);
                        }
                    }
                    if (ringItemData.size() > 0) {
                        // 交给线程池
                        for (Integer jobId : ringItemData) {
                            JobTriggerPoolHelper.trigger(jobId);
                        }
                    }
                } catch (Exception e) {
                    if (!ringThreadToStop) {
                        logger.error(">>>>>>>>>>> ScheduledTask, JobScheduleHelper#ringThread error:{}", e);
                    }
                }
            }
        });


        ringThread.start();
        ;

    }

    /**
     * 将定时任务放入时间轮中
     *
     * @param ringSecond 时间轮刻度
     * @param jobId      任务id
     */
    private void pushTimeRing(int ringSecond, int jobId) {
        List<Integer> ringItemData = ringData.get(ringSecond);
        if (ObjectUtil.isNull(ringItemData)) {
            ringItemData = new ArrayList<>();
            ringData.put(ringSecond, ringItemData);
        }
        ringItemData.add(jobId);

    }


    private void refreshNextValidTime(ScheduledTaskInfo jobInfo, Date fromTime) throws ParseException {
        Date nextValidTime = new CronExpression(jobInfo.getScheduleConf()).getNextValidTimeAfter(fromTime);
        // 如果计算的下次执行时间不为空
        if (ObjectUtil.isNotNull(nextValidTime)) {
            jobInfo.setTriggerLastTime(fromTime.getTime());
            jobInfo.setTriggerNextTime(nextValidTime.getTime());
        } else {
            // 如果为空，说明cron表达式可能为空，或者该任务已经置为删除状态
            jobInfo.setTriggerStatus(0);
            jobInfo.setTriggerLastTime(0);
            jobInfo.setTriggerNextTime(0);

        }

    }


}
