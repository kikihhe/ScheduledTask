package com.xiaohe.admin.service.impl;

import com.xiaohe.admin.core.complete.XxlJobCompleter;
import com.xiaohe.admin.core.cron.CronExpression;
import com.xiaohe.admin.core.model.XxlJobGroup;
import com.xiaohe.admin.core.model.XxlJobInfo;
import com.xiaohe.admin.core.model.XxlJobLogReport;
import com.xiaohe.core.route.ExecutorRouterStrategyEnum;
import com.xiaohe.admin.core.scheduler.MisfireStrategyEnum;
import com.xiaohe.admin.core.scheduler.ScheduleTypeEnum;
import com.xiaohe.admin.core.thread.JobScheduleHelper;
import com.xiaohe.admin.core.util.I18nUtil;
import com.xiaohe.admin.mapper.*;
import com.xiaohe.admin.service.XxlJobService;
import com.xiaohe.core.enums.ExecutorBlockStrategyEnum;
import com.xiaohe.core.model.Result;
import com.xiaohe.core.util.DateUtil;
import com.xiaohe.core.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.util.*;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-09-27 13:59
 */
@Service
public class XxlJobServiceImpl implements XxlJobService {

    private static Logger logger = LoggerFactory.getLogger(XxlJobServiceImpl.class);
    @Resource
    private XxlJobInfoMapper xxlJobInfoMapper;

    @Resource
    private XxlJobGroupMapper xxlJobGroupMapper;

    @Resource
    private XxlJobLogMapper xxlJobLogMapper;

    @Resource
    private XxlJobLogGlueMapper xxlJobLogGlueMapper;

    @Resource
    private XxlJobLogReportMapper xxlJobLogReportMapper;


    @Override
    public Map<String, Object> pageList(int start, int length, int jobGroup, int triggerStatus, String jobDesc, String executorHandler, String author) {
        List<XxlJobInfo> xxlJobInfos = xxlJobInfoMapper.pageList(start, length, jobGroup, triggerStatus, jobDesc, executorHandler, author);
        Map<String, Object> map = new HashMap<>();
        map.put("recordsTotal", xxlJobInfos.size());
        map.put("recordsFiltered", xxlJobInfos.size());
        map.put("data", xxlJobInfos);
        return map;
    }

    public Result<String> addBatch(List<XxlJobInfo> xxlJobInfos) {
        for (XxlJobInfo xxlJobInfo : xxlJobInfos) {
            Result<String> add = add(xxlJobInfo);
            if (add.getCode() != 200) {
                return add;
            }
        }
        return Result.success();
    }

    /**
     * 添加定时任务
     *
     * @param jobInfo
     */
    @Override
    public Result<String> add(XxlJobInfo jobInfo) {
        //先查询到该定时任务对应的执行器
        XxlJobGroup group = xxlJobGroupMapper.load(jobInfo.getJobGroup());
        if (group == null) {
            //如果执行器为空，返回失败
            return new Result<String>(Result.FAIL_CODE, (I18nUtil.getString("system_please_choose") + I18nUtil.getString("jobinfo_field_jobgroup")));
        }
        //下面是两个判空操作，先判空定时任务描述
        if (jobInfo.getJobDesc() == null || jobInfo.getJobDesc().trim().length() == 0) {
            return new Result<String>(Result.FAIL_CODE, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_field_jobdesc")));
        }
        //再判空定时任务负责人
        if (jobInfo.getAuthor() == null || jobInfo.getAuthor().trim().length() == 0) {
            return new Result<String>(Result.FAIL_CODE, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_field_author")));
        }
        //判断前段发送的定时任务是哪种调度类型的
        ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(jobInfo.getScheduleType(), null);
        if (scheduleTypeEnum == null) {
            //如果为空，则返回失败
            return new Result<String>(Result.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
        }
        //判断是否为cron调度类型
        if (scheduleTypeEnum == ScheduleTypeEnum.CRON) {
            //如果是cron，则判断cron表达式是否正确
            if (jobInfo.getScheduleConf() == null || !CronExpression.isValidExpression(jobInfo.getScheduleConf())) {
                return new Result<String>(Result.FAIL_CODE, "Cron" + I18nUtil.getString("system_unvalid"));
            }
        }
        //如果调度类型为按照固定频率
        else if (scheduleTypeEnum == ScheduleTypeEnum.FIX_RATE) {
            if (jobInfo.getScheduleConf() == null) {
                //如果调度规则为空则返回失败
                return new Result<String>(Result.FAIL_CODE, (I18nUtil.getString("schedule_type")));
            }
            try {
                //如果规则的值小于1，则返回失败
                int fixSecond = Integer.valueOf(jobInfo.getScheduleConf());
                if (fixSecond < 1) {
                    return new Result<String>(Result.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
                }
            } catch (Exception e) {
                return new Result<String>(Result.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
            }
        }


        //判断路由策略是否为空
        if (ExecutorRouterStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null) == null) {
            return new Result<String>(Result.FAIL_CODE, (I18nUtil.getString("jobinfo_field_executorRouteStrategy") + I18nUtil.getString("system_unvalid")));
        }
        //判断调度失败策略是否为空
        if (MisfireStrategyEnum.match(jobInfo.getMisfireStrategy(), null) == null) {
            return new Result<String>(Result.FAIL_CODE, (I18nUtil.getString("misfire_strategy") + I18nUtil.getString("system_unvalid")));
        }
        //判断阻塞策略是否为空
        if (ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), null) == null) {
            return new Result<String>(Result.FAIL_CODE, (I18nUtil.getString("jobinfo_field_executorBlockStrategy") + I18nUtil.getString("system_unvalid")));
        }
        //判断是否有子任务
        if (jobInfo.getChildJobId() != null && jobInfo.getChildJobId().trim().length() > 0) {
            String[] childJobIds = jobInfo.getChildJobId().split(",");
            //如果有则遍历子任务，做相应的判断处理
            for (String childJobIdItem : childJobIds) {
                if (childJobIdItem != null && childJobIdItem.trim().length() > 0 && XxlJobCompleter.isNumber(childJobIdItem)) {
                    XxlJobInfo childJobInfo = xxlJobInfoMapper.loadById(Integer.parseInt(childJobIdItem));
                    if (childJobInfo == null) {
                        return new Result<String>(Result.FAIL_CODE,
                                MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil.getString("system_not_found")), childJobIdItem));
                    }
                } else {
                    return new Result<String>(Result.FAIL_CODE,
                            MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil.getString("system_unvalid")), childJobIdItem));
                }
            }
            //这里是把所有子任务id拼到一块
            String temp = "";
            for (String item : childJobIds) {
                temp += item + ",";
            }
            //去掉最后一个句号
            temp = temp.substring(0, temp.length() - 1);
            //设置子任务id
            jobInfo.setChildJobId(temp);
        }
        //下面就是定时任务的添加时间，更新时间和glue的更新时间
        jobInfo.setAddTime(new Date());
        jobInfo.setUpdateTime(new Date());
        jobInfo.setGlueUpdatetime(new Date());
        //真正保存定时任务
        xxlJobInfoMapper.save(jobInfo);
        if (jobInfo.getId() < 1) {
            //走到这里意味保存失败
            return new Result<String>(Result.FAIL_CODE, (I18nUtil.getString("jobinfo_field_add") + I18nUtil.getString("system_fail")));
        }
        //走到这里则保存成功
        return new Result<String>(String.valueOf(jobInfo.getId()));
    }

    /**
     * 更新定时任务的方法，
     */
    @Override
    public Result<String> update(XxlJobInfo jobInfo) {
        if (!StringUtil.hasText(jobInfo.getJobDesc())) {
            return new Result<String>(Result.FAIL_CODE, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_field_jobdesc")));
        }
        if (jobInfo.getAuthor() == null || jobInfo.getAuthor().trim().isEmpty()) {
            return new Result<String>(Result.FAIL_CODE, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_field_author")));
        }
        ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(jobInfo.getScheduleType(), null);
        if (scheduleTypeEnum == null) {
            return new Result<String>(Result.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
        }
        if (scheduleTypeEnum == ScheduleTypeEnum.CRON) {
            if (jobInfo.getScheduleConf() == null || !CronExpression.isValidExpression(jobInfo.getScheduleConf())) {
                return new Result<String>(Result.FAIL_CODE, "Cron" + I18nUtil.getString("system_unvalid"));
            }
        } else if (scheduleTypeEnum == ScheduleTypeEnum.FIX_RATE /*|| scheduleTypeEnum == ScheduleTypeEnum.FIX_DELAY*/) {
            if (jobInfo.getScheduleConf() == null) {
                return new Result<String>(Result.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
            }
            try {
                int fixSecond = Integer.valueOf(jobInfo.getScheduleConf());
                if (fixSecond < 1) {
                    return new Result<String>(Result.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
                }
            } catch (Exception e) {
                return new Result<String>(Result.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
            }
        }
        if (ExecutorRouterStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null) == null) {
            return new Result<String>(Result.FAIL_CODE, (I18nUtil.getString("jobinfo_field_executorRouteStrategy") + I18nUtil.getString("system_unvalid")));
        }
        if (MisfireStrategyEnum.match(jobInfo.getMisfireStrategy(), null) == null) {
            return new Result<String>(Result.FAIL_CODE, (I18nUtil.getString("misfire_strategy") + I18nUtil.getString("system_unvalid")));
        }
        if (ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), null) == null) {
            return new Result<String>(Result.FAIL_CODE, (I18nUtil.getString("jobinfo_field_executorBlockStrategy") + I18nUtil.getString("system_unvalid")));
        }
        if (jobInfo.getChildJobId() != null && !jobInfo.getChildJobId().trim().isEmpty()) {
            String[] childJobIds = jobInfo.getChildJobId().split(",");
            for (String childJobIdItem : childJobIds) {
                if (childJobIdItem != null && !childJobIdItem.trim().isEmpty() && XxlJobCompleter.isNumber(childJobIdItem)) {
                    XxlJobInfo childJobInfo = xxlJobInfoMapper.loadById(Integer.parseInt(childJobIdItem));
                    if (childJobInfo == null) {
                        return new Result<String>(Result.FAIL_CODE,
                                MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil.getString("system_not_found")), childJobIdItem));
                    }
                } else {
                    return new Result<String>(Result.FAIL_CODE,
                            MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil.getString("system_unvalid")), childJobIdItem));
                }
            }
            String temp = "";
            for (String item : childJobIds) {
                temp += item + ",";
            }
            temp = temp.substring(0, temp.length() - 1);
            jobInfo.setChildJobId(temp);
        }
        XxlJobGroup jobGroup = xxlJobGroupMapper.load(jobInfo.getJobGroup());
        if (jobGroup == null) {
            return new Result<String>(Result.FAIL_CODE, (I18nUtil.getString("jobinfo_field_jobgroup") + I18nUtil.getString("system_unvalid")));
        }
        //从数据库中查询出旧的定时任务信息
        XxlJobInfo exists_jobInfo = xxlJobInfoMapper.loadById(jobInfo.getId());
        if (exists_jobInfo == null) {
            return new Result<String>(Result.FAIL_CODE, (I18nUtil.getString("jobinfo_field_id") + I18nUtil.getString("system_not_found")));
        }
        //既然是更新定时任务，下面就要做点不一样的事，先得到定时任务下一次的执行时间
        long nextTriggerTime = exists_jobInfo.getTriggerNextTime();
        //判断调度类型是不是和数据库中存储的相同
        boolean scheduleDataNotChanged = jobInfo.getScheduleType().equals(exists_jobInfo.getScheduleType()) && jobInfo.getScheduleConf().equals(exists_jobInfo.getScheduleConf());
        //如果调度类型不一样，并且定时任务现在处于运行的状态，想想你修改定时任务的cron表达式，它就会在下面这里生效
        if (exists_jobInfo.getTriggerStatus() == 1 && !scheduleDataNotChanged) {
            try {
                //根据新的cron表达式，计算定时任务下一次的执行时间。但这里有一个条件，就是从当前时间加5秒之后的定时任务的
                //执行时间，这么做其实就是在一个新的调度周期中，开始以新的执行时间来调度定时任务
                Date nextValidTime = JobScheduleHelper.generateNextValidTime(jobInfo, new Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS));
                if (nextValidTime == null) {
                    return new Result<String>(Result.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
                }
                //把新的执行时间赋值给上面的nextTriggerTime
                nextTriggerTime = nextValidTime.getTime();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                return new Result<String>(Result.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
            }
        }
        //更新旧的定时任务的信息
        exists_jobInfo.setJobGroup(jobInfo.getJobGroup());
        exists_jobInfo.setJobDesc(jobInfo.getJobDesc());
        exists_jobInfo.setAuthor(jobInfo.getAuthor());
        exists_jobInfo.setAlarmEmail(jobInfo.getAlarmEmail());
        exists_jobInfo.setScheduleType(jobInfo.getScheduleType());
        exists_jobInfo.setScheduleConf(jobInfo.getScheduleConf());
        exists_jobInfo.setMisfireStrategy(jobInfo.getMisfireStrategy());
        exists_jobInfo.setExecutorRouteStrategy(jobInfo.getExecutorRouteStrategy());
        exists_jobInfo.setExecutorHandler(jobInfo.getExecutorHandler());
        exists_jobInfo.setExecutorParam(jobInfo.getExecutorParam());
        exists_jobInfo.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
        exists_jobInfo.setExecutorTimeout(jobInfo.getExecutorTimeout());
        exists_jobInfo.setExecutorFailRetryCount(jobInfo.getExecutorFailRetryCount());
        exists_jobInfo.setChildJobId(jobInfo.getChildJobId());
        exists_jobInfo.setTriggerNextTime(nextTriggerTime);
        exists_jobInfo.setUpdateTime(new Date());
        //更新定时任务
        xxlJobInfoMapper.update(exists_jobInfo);
        return Result.SUCCESS;
    }

    @Override
    public Result remove(Integer id) {
        XxlJobInfo xxlJobInfo = xxlJobInfoMapper.loadById(id);
        if (xxlJobInfo == null) {
            return Result.success();
        }
        xxlJobInfoMapper.delete(id);
        xxlJobLogMapper.delete(id);
        xxlJobLogGlueMapper.deleteByJobId(id);
        return Result.success();
    }

    /**
     * 获取运行报表页面需要的所有数据
     */
    @Override
    public Map<String, Object> dashboardInfo() {
        int jobInfoCount = xxlJobInfoMapper.findAllCount();
        int jobLogCount = 0;
        int jobLogSuccessCount = 0;
        XxlJobLogReport xxlJobLogReport = xxlJobLogReportMapper.queryLogReportTotal();
        if (xxlJobLogReport != null) {
            jobLogCount = xxlJobLogReport.getRunningCount() + xxlJobLogReport.getSucCount() + xxlJobLogReport.getFailCount();
            jobLogSuccessCount = xxlJobLogReport.getSucCount();
        }
        Set<String> executorAddressSet = new HashSet<String>();
        List<XxlJobGroup> groupList = xxlJobGroupMapper.findAll();
        if (groupList != null && !groupList.isEmpty()) {
            for (XxlJobGroup group : groupList) {
                if (group.getRegistryList() != null && !group.getRegistryList().isEmpty()) {
                    executorAddressSet.addAll(group.getRegistryList());
                }
            }
        }
        int executorCount = executorAddressSet.size();
        Map<String, Object> dashboardMap = new HashMap<String, Object>();
        dashboardMap.put("jobInfoCount", jobInfoCount);
        dashboardMap.put("jobLogCount", jobLogCount);
        dashboardMap.put("jobLogSuccessCount", jobLogSuccessCount);
        dashboardMap.put("executorCount", executorCount);
        return dashboardMap;
    }

    public Result chartInfo(Date startDate, Date endDate) {

        List<String> triggerDayList = new ArrayList<String>();
        List<Integer> triggerDayCountRunningList = new ArrayList<Integer>();
        List<Integer> triggerDayCountSucList = new ArrayList<Integer>();
        List<Integer> triggerDayCountFailList = new ArrayList<Integer>();
        int triggerCountRunningTotal = 0;
        int triggerCountSucTotal = 0;
        int triggerCountFailTotal = 0;
        List<XxlJobLogReport> logReportList = xxlJobLogReportMapper.queryLogReport(startDate, endDate);
        if (logReportList != null && logReportList.size() > 0) {
            for (XxlJobLogReport item : logReportList) {
                String day = DateUtil.formatDate(item.getTriggerDay());
                int triggerDayCountRunning = item.getRunningCount();
                int triggerDayCountSuc = item.getSucCount();
                int triggerDayCountFail = item.getFailCount();
                triggerDayList.add(day);
                triggerDayCountRunningList.add(triggerDayCountRunning);
                triggerDayCountSucList.add(triggerDayCountSuc);
                triggerDayCountFailList.add(triggerDayCountFail);
                triggerCountRunningTotal += triggerDayCountRunning;
                triggerCountSucTotal += triggerDayCountSuc;
                triggerCountFailTotal += triggerDayCountFail;
            }
        } else {
            for (int i = -6; i <= 0; i++) {
                triggerDayList.add(DateUtil.formatDate(DateUtil.addDays(new Date(), i)));
                triggerDayCountRunningList.add(0);
                triggerDayCountSucList.add(0);
                triggerDayCountFailList.add(0);
            }
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("triggerDayList", triggerDayList);
        result.put("triggerDayCountRunningList", triggerDayCountRunningList);
        result.put("triggerDayCountSucList", triggerDayCountSucList);
        result.put("triggerDayCountFailList", triggerDayCountFailList);
        result.put("triggerCountRunningTotal", triggerCountRunningTotal);
        result.put("triggerCountSucTotal", triggerCountSucTotal);
        result.put("triggerCountFailTotal", triggerCountFailTotal);
        return Result.success(result);
    }

    /**
     * 启动定时任务，这里启动定时任务，其实就是计算出最新的定时任务可以被调度的时间 至于怎么被调度，最有线程去做这件事
     */
    @Override
    public Result<String> start(int id) {
        //得到定时任务
        XxlJobInfo xxlJobInfo = xxlJobInfoMapper.loadById(id);
        //得到调度类型
        ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(xxlJobInfo.getScheduleType(), ScheduleTypeEnum.NONE);
        if (ScheduleTypeEnum.NONE == scheduleTypeEnum) {
            //调度类型为空，就是什么也不使用，就什么都不做，不调度该任务
            return new Result<String>(Result.FAIL_CODE, (I18nUtil.getString("schedule_type_none_limit_start")) );
        }
        long nextTriggerTime = 0;
        try {
            //得到该定时任务5秒之后的执行时间，这里之所以要得到5秒之后，是因为调度定时任务的线程，刚开始执行的时候会睡4到5秒
            Date nextValidTime = JobScheduleHelper.generateNextValidTime(xxlJobInfo, new Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS));
            if (nextValidTime == null) {
                return new Result<String>(Result.FAIL_CODE, (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid")) );
            }
            //下一次执行时间赋值
            nextTriggerTime = nextValidTime.getTime();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new Result<String>(Result.FAIL_CODE, (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid")) );
        }
        //修稿定时任务的运行状态，改为运行
        xxlJobInfo.setTriggerStatus(1);
        xxlJobInfo.setTriggerLastTime(0);
        xxlJobInfo.setTriggerNextTime(nextTriggerTime);
        xxlJobInfo.setUpdateTime(new Date());
        xxlJobInfoMapper.update(xxlJobInfo);
        return Result.SUCCESS;
    }


    /**
     * 停止定时任务
     */
    @Override
    public Result<String> stop(int id) {
        XxlJobInfo xxlJobInfo = xxlJobInfoMapper.loadById(id);
        xxlJobInfo.setTriggerStatus(0);
        xxlJobInfo.setTriggerLastTime(0);
        // 把下一次执行时间置为0了
        xxlJobInfo.setTriggerNextTime(0);
        xxlJobInfo.setUpdateTime(new Date());
        xxlJobInfoMapper.update(xxlJobInfo);
        return Result.SUCCESS;
    }
}
