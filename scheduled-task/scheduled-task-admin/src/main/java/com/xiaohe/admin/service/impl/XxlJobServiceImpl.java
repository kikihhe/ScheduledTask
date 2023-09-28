package com.xiaohe.admin.service.impl;

import com.xiaohe.admin.core.cron.CronExpression;
import com.xiaohe.admin.core.model.XxlJobGroup;
import com.xiaohe.admin.core.model.XxlJobInfo;
import com.xiaohe.admin.core.scheduler.ScheduleTypeEnum;
import com.xiaohe.admin.mapper.XxlJobGroupMapper;
import com.xiaohe.admin.mapper.XxlJobInfoMapper;
import com.xiaohe.admin.mapper.XxlJobLogGlueMapper;
import com.xiaohe.admin.mapper.XxlJobLogMapper;
import com.xiaohe.admin.service.XxlJobService;
import com.xiaohe.core.model.Result;
import com.xiaohe.core.util.StringUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-09-27 13:59
 */
@Service
public class XxlJobServiceImpl implements XxlJobService {
    @Resource
    private XxlJobInfoMapper xxlJobInfoMapper;

    @Resource
    private XxlJobGroupMapper xxlJobGroupMapper;

    @Resource
    private XxlJobLogMapper xxlJobLogMapper;

    @Resource
    private XxlJobLogGlueMapper xxlJobLogGlueMapper;



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
        // TODO 判断其他的策略是否符合格式

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
}
