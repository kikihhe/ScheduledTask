package com.xiaohe.ScheduledTask.admin.core.thread;

import com.xiaohe.ScheduledTask.admin.core.cron.CronExpression;
import com.xiaohe.ScheduledTask.admin.core.model.ScheduledTaskInfo;
import com.xiaohe.ScheduledTask.admin.core.util.ObjectUtil;
import com.xiaohe.ScheduledTask.admin.dao.ScheduledTaskInfoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
                long nowTime = System.currentTimeMillis();
                // 查询5s内可执行的任务, (每次最多取6000个，这里其实是配置的，先写死)
                List<ScheduledTaskInfo> jobInfoList = scheduledTaskInfoMapper.scheduleJobQuery(nowTime + PRE_READ_MS, 6000);
                for (ScheduledTaskInfo jobInfo : jobInfoList) {
                    // 计算该任务应放在哪个刻度上
                    int ringSecond = (int) (jobInfo.getTriggerNextTime() / 1000 % 60);
                    // 将定时任务id放入该刻度的List集合中
                    pushTimeRing(ringSecond, jobInfo.getId());
                    // 更新时间
                    Date nextTime = new CronExpression(jobInfo.getScheduleConf()).getNextValidTimeAfter(new Date());
                    jobInfo.setTriggerNextTime(nextTime.getTime());
                    scheduledTaskInfoMapper.scheduleUpdate(jobInfo);
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
                   if(!ringThreadToStop) {
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


        ringThread.start();;

    }

    /**
     * 将定时任务放入时间轮中
     * @param ringSecond 时间轮刻度
     * @param jobId 任务id
     */
    private void pushTimeRing(int ringSecond, int jobId) {
        List<Integer> ringItemData = ringData.get(ringSecond);
        if (ObjectUtil.isNull(ringItemData)) {
            ringItemData = new ArrayList<>();
            ringData.put(ringSecond, ringItemData);
        }
        ringItemData.add(jobId);

    }







}
