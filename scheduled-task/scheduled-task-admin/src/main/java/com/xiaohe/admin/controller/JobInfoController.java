package com.xiaohe.admin.controller;


import com.xiaohe.admin.core.model.XxlJobGroup;
import com.xiaohe.admin.core.route.ExecutorRouterStrategyEnum;
import com.xiaohe.admin.core.scheduler.MisfireStrategyEnum;
import com.xiaohe.admin.core.scheduler.ScheduleTypeEnum;
import com.xiaohe.admin.core.thread.JobScheduleHelper;
import com.xiaohe.admin.core.thread.JobTriggerPoolHelper;
import com.xiaohe.admin.core.trigger.TriggerTypeEnum;
import com.xiaohe.admin.core.util.I18nUtil;
import com.xiaohe.admin.mapper.XxlJobGroupMapper;
import com.xiaohe.admin.service.XxlJobService;
import com.xiaohe.core.enums.ExecutorBlockStrategyEnum;
import com.xiaohe.core.model.Result;
import com.xiaohe.core.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.xiaohe.admin.service.*;
import com.xiaohe.admin.core.model.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 任务管理页面，对这个页面操作的方法，都在这个类中
 */
@Controller
@RequestMapping("/jobinfo")
public class JobInfoController {
    private static Logger logger = LoggerFactory.getLogger(JobInfoController.class);

    @Resource
    private XxlJobGroupMapper xxlJobGroupMapper;
    @Resource
    private XxlJobService xxlJobService;


    /**
     * 查询该界面需要的所有数据
     */
    @RequestMapping
    public String index(HttpServletRequest request, Model model, @RequestParam(required = false, defaultValue = "-1") int jobGroup) {
        model.addAttribute("ExecutorRouteStrategyEnum", ExecutorRouterStrategyEnum.values());
        model.addAttribute("ExecutorBlockStrategyEnum", ExecutorBlockStrategyEnum.values());
        model.addAttribute("ScheduleTypeEnum", ScheduleTypeEnum.values());
        model.addAttribute("MisfireStrategyEnum", MisfireStrategyEnum.values());
        List<XxlJobGroup> jobGroupList_all =  xxlJobGroupMapper.findAll();
        List<XxlJobGroup> jobGroupList = filterJobGroupByRole(request, jobGroupList_all);
        if (jobGroupList==null || jobGroupList.size()==0) {
            throw new RuntimeException(I18nUtil.getString("jobgroup_empty"));
        }
        model.addAttribute("JobGroupList", jobGroupList);
        model.addAttribute("jobGroup", jobGroup);
        return "jobinfo/jobinfo.index";
    }


    /**
     * 根据用户角色查找执行器的方法
     */
    public static List<XxlJobGroup> filterJobGroupByRole(HttpServletRequest request, List<XxlJobGroup> jobGroupList_all) {
        List<XxlJobGroup> jobGroupList = new ArrayList<>();
        if (jobGroupList_all != null && jobGroupList_all.size() > 0) {
            XxlJobUser loginUser = (XxlJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
            if (loginUser.getRole() == 1) {
                jobGroupList = jobGroupList_all;
            } else {
                List<String> groupIdStrs = new ArrayList<>();
                if (loginUser.getPermission() != null && loginUser.getPermission().trim().length() > 0) {
                    groupIdStrs = Arrays.asList(loginUser.getPermission().trim().split(","));
                }
                for (XxlJobGroup groupItem : jobGroupList_all) {
                    if (groupIdStrs.contains(String.valueOf(groupItem.getId()))) {
                        jobGroupList.add(groupItem);
                    }
                }
            }
        }
        return jobGroupList;
    }


    /**
     * 校验当前用户是否有某个执行器的权限
     */
    public static void validPermission(HttpServletRequest request, int jobGroup) {
        XxlJobUser loginUser = (XxlJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
        if (!loginUser.validPermission(jobGroup)) {
            throw new RuntimeException(I18nUtil.getString("system_permission_limit") + "[username=" + loginUser.getUsername() + "]");
        }
    }


    /**
     * 分页查询定时任务
     */
    @RequestMapping("/pageList")
    @ResponseBody
    public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        int jobGroup, int triggerStatus, String jobDesc, String executorHandler, String author) {
        return xxlJobService.pageList(start, length, jobGroup, triggerStatus, jobDesc, executorHandler, author);
    }


    /**
     * 新增一个定时任务
     */
    @RequestMapping("/add")
    @ResponseBody
    public Result<String> add(XxlJobInfo jobInfo) {
        return xxlJobService.add(jobInfo);
    }

    @RequestMapping("/addBatch")
    @ResponseBody
    public Result<String> addBatch(List<XxlJobInfo> xxlJobInfos) {
        return xxlJobService.addBatch(xxlJobInfos);
    }
    /**
     * 更新定时任务
     */
    @RequestMapping("/update")
    @ResponseBody
    public Result<String> update(XxlJobInfo jobInfo) {
        return xxlJobService.update(jobInfo);
    }


    /**
     * 删除定时任务
     */
    @RequestMapping("/remove")
    @ResponseBody
    public Result<String> remove(int id) {
        return xxlJobService.remove(id);
    }


    /**
     * 停止定时任务
     */
    @RequestMapping("/stop")
    @ResponseBody
    public Result<String> pause(int id) {
        return xxlJobService.stop(id);
    }


    /**
     * 启动定时任务
     */
    @RequestMapping("/start")
    @ResponseBody
    public Result<String> start(int id) {
        return xxlJobService.start(id);
    }


    /**
     * 只执行一次定时任务
     */
    @RequestMapping("/trigger")
    @ResponseBody
    public Result<String> triggerJob(int id, String executorParam, String addressList) {
        // force cover job param
        if (executorParam == null) {
            executorParam = "";
        }
        //这里任务就是手动触发的
        JobTriggerPoolHelper.trigger(id, TriggerTypeEnum.MANUAL, -1, null, executorParam, addressList);
        return Result.SUCCESS;
    }


    /**
     * 获取任务下一次的执行时间
     */
    @RequestMapping("/nextTriggerTime")
    @ResponseBody
    public Result<List<String>> nextTriggerTime(String scheduleType, String scheduleConf) {
        XxlJobInfo paramXxlJobInfo = new XxlJobInfo();
        paramXxlJobInfo.setScheduleType(scheduleType);
        paramXxlJobInfo.setScheduleConf(scheduleConf);
        List<String> result = new ArrayList<>();
        try {
            Date lastTime = new Date();
            for (int i = 0; i < 5; i++) {
                lastTime = JobScheduleHelper.generateNextValidTime(paramXxlJobInfo, lastTime);
                if (lastTime != null) {
                    result.add(DateUtil.formatDateTime(lastTime));
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new Result<List<String>>(Result.FAIL_CODE, (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid")) + e.getMessage());
        }
        return new Result<List<String>>(result);
    }
}
