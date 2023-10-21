package com.xiaohe.admin.service.impl;

import com.xiaohe.admin.core.cron.CronExpression;
import com.xiaohe.admin.core.model.XxlJobGroup;
import com.xiaohe.admin.core.model.XxlJobInfo;
import com.xiaohe.admin.core.model.XxlJobLogReport;
import com.xiaohe.admin.core.scheduler.ScheduleTypeEnum;
import com.xiaohe.admin.core.thread.JobScheduleHelper;
import com.xiaohe.admin.mapper.*;
import com.xiaohe.admin.service.XxlJobService;
import com.xiaohe.core.model.Result;
import com.xiaohe.core.util.DateUtil;
import com.xiaohe.core.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
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

    /**
     * 添加定时任务
     *
     * @param jobInfo
     */
    @Override
    public Result add(XxlJobInfo jobInfo) {
        // 开始做数据校验
        XxlJobGroup xxlJobGroup = xxlJobGroupMapper.load(jobInfo.getJobGroup());
        if (xxlJobGroup == null) {
            return Result.error("system_please_choose jobinfo_field_jobgroup");
        }
        if (!StringUtil.hasText(jobInfo.getJobDesc())) {
            return Result.error("system_please_input jobinfo_field_jobdesc");
        }
        if (!StringUtil.hasText(jobInfo.getAuthor())) {
            return Result.error("system_please_input jobinfo_field_author");
        }
        // 判断调度类型
        ScheduleTypeEnum scheduleType = ScheduleTypeEnum.match(jobInfo.getScheduleType(), null);
        if (scheduleType == null) {
            return Result.error("schedule_type system_unvalid");
        }
        if (scheduleType == ScheduleTypeEnum.CRON && jobInfo.getScheduleConf().isEmpty() || !CronExpression.isValidExpression(jobInfo.getScheduleConf())) {
            return Result.error("cron system_unvalid");
        }
        if (scheduleType == ScheduleTypeEnum.FIX_RATE) {
            if (jobInfo.getScheduleConf().isEmpty()) {
                return Result.error("schedule_type");
            }
            // 如果填写的频率小于1，也返回失败
            try {
                int i = Integer.parseInt(jobInfo.getScheduleConf());
                if (i < 1) {
                    return Result.error("schedule_type system_unvalid");
                }
            } catch (Exception e) {
                return Result.error("schedule_type system_unvalid");
            }
        }


        // 开始添加
        jobInfo.setAddTime(new Date());
        jobInfo.setUpdateTime(new Date());
        jobInfo.setGlueUpdatetime(new Date());
        int save = xxlJobInfoMapper.save(jobInfo);
        if (save < 1 || jobInfo.getId() < 0) {
            return Result.error("jobinfo_field_add system_fail");
        }

        return Result.success("add success");
    }

    @Override
    public Result<String> update(XxlJobInfo jobInfo) {
        // TODO 更新定时任务
        return null;
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
            return new Result<String>(Result.FAIL_CODE, ("schedule_type_none_limit_start"));
        }
        long nextTriggerTime = 0;
        try {
            // 得到该定时任务5秒之后的执行时间，这里之所以要得到5秒之后，是因为调度定时任务的线程，刚开始执行的时候会睡4到5秒
            Date nextValidTime = JobScheduleHelper.generateNextValidTime(xxlJobInfo, new Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS));
            if (nextValidTime == null) {
                return new Result<String>(Result.FAIL_CODE, "schedule_type" + "system_unvalid");
            }
            //下一次执行时间赋值
            nextTriggerTime = nextValidTime.getTime();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new Result<>(Result.FAIL_CODE, "schedule_type" + "system_unvalid");
        }
        //修改定时任务的运行状态，改为运行
        xxlJobInfo.setTriggerStatus(1);
        xxlJobInfo.setTriggerLastTime(0);
        xxlJobInfo.setTriggerNextTime(nextTriggerTime);
        xxlJobInfo.setUpdateTime(new Date());
        xxlJobInfoMapper.update(xxlJobInfo);
        return Result.SUCCESS;
    }

}
