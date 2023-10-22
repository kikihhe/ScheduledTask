package com.xiaohe.admin.controller;

import com.xiaohe.admin.core.complete.XxlJobCompleter;
import com.xiaohe.admin.core.model.*;
import com.xiaohe.admin.core.scheduler.XxlJobScheduler;
import com.xiaohe.admin.core.util.I18nUtil;
import com.xiaohe.admin.mapper.XxlJobGroupMapper;
import com.xiaohe.admin.mapper.XxlJobInfoMapper;
import com.xiaohe.admin.mapper.XxlJobLogMapper;
import com.xiaohe.core.biz.ExecutorBiz;
import com.xiaohe.core.model.KillParam;
import com.xiaohe.core.model.LogParam;
import com.xiaohe.core.model.LogResult;
import com.xiaohe.core.model.Result;
import com.xiaohe.core.util.CollectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.xiaohe.core.util.DateUtil;

/**
 * 获得日志信息的类，这个对对应的就是调度日志界面
 */
@Controller
@RequestMapping("/joblog")
public class JobLogController {
    private static Logger logger = LoggerFactory.getLogger(JobLogController.class);

    @Resource
    private XxlJobGroupMapper xxlJobGroupMapper;
    @Resource
    public XxlJobInfoMapper xxlJobInfoMapper;
    @Resource
    public XxlJobLogMapper xxlJobLogMapper;


    @RequestMapping
    public String index(HttpServletRequest request, Model model, @RequestParam(required = false, defaultValue = "0") Integer jobId) {
        List<XxlJobGroup> jobGroupList_all = xxlJobGroupMapper.findAll();
        List<XxlJobGroup> jobGroupList = JobInfoController.filterJobGroupByRole(request, jobGroupList_all);
        if (jobGroupList == null || jobGroupList.size() == 0) {
            throw new RuntimeException(I18nUtil.getString("jobgroup_empty"));
        }
        model.addAttribute("JobGroupList", jobGroupList);
        if (jobId > 0) {
            XxlJobInfo jobInfo = xxlJobInfoMapper.loadById(jobId);
            if (jobInfo == null) {
                throw new RuntimeException(I18nUtil.getString("jobinfo_field_id") + I18nUtil.getString("system_unvalid"));
            }
            model.addAttribute("jobInfo", jobInfo);
            JobInfoController.validPermission(request, jobInfo.getJobGroup());
        }
        return "joblog/joblog.index";
    }


    @RequestMapping("/getJobsByGroup")
    @ResponseBody
    public Result<List<XxlJobInfo>> getJobsByGroup(int jobGroup) {
        List<XxlJobInfo> list = xxlJobInfoMapper.getJobsByGroup(jobGroup);
        return new Result<List<XxlJobInfo>>(list);
    }


    @RequestMapping("/pageList")
    @ResponseBody
    public Map<String, Object> pageList(HttpServletRequest request,
                                        @RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        int jobGroup, int jobId, int logStatus, String filterTime) {
        JobInfoController.validPermission(request, jobGroup);
        Date triggerTimeStart = null;
        Date triggerTimeEnd = null;
        if (filterTime != null && filterTime.trim().length() > 0) {
            String[] temp = filterTime.split(" - ");
            if (temp.length == 2) {
                triggerTimeStart = DateUtil.parseDateTime(temp[0]);
                triggerTimeEnd = DateUtil.parseDateTime(temp[1]);
            }
        }
        List<XxlJobLog> list = xxlJobLogMapper.pageList(start, length, jobGroup, jobId, triggerTimeStart, triggerTimeEnd, logStatus);
        int list_count = xxlJobLogMapper.pageListCount(start, length, jobGroup, jobId, triggerTimeStart, triggerTimeEnd, logStatus);
        Map<String, Object> maps = new HashMap<String, Object>();
        maps.put("recordsTotal", list_count);
        maps.put("recordsFiltered", list_count);
        maps.put("data", list);
        return maps;
    }


    @RequestMapping("/logDetailPage")
    public String logDetailPage(int id, Model model) {
        Result<String> logStatue = Result.SUCCESS;
        XxlJobLog jobLog = xxlJobLogMapper.load(id);
        if (jobLog == null) {
            throw new RuntimeException(I18nUtil.getString("joblog_logid_unvalid"));
        }
        model.addAttribute("triggerCode", jobLog.getTriggerCode());
        model.addAttribute("handleCode", jobLog.getHandlerCode());
        model.addAttribute("executorAddress", jobLog.getExecutorAddress());
        model.addAttribute("triggerTime", jobLog.getTriggerTime().getTime());
        model.addAttribute("logId", jobLog.getId());
        return "joblog/joblog.detail";
    }


    @RequestMapping("/logDetailCat")
    @ResponseBody
    public Result<LogResult> logDetailCat(String executorAddress, long triggerTime, long logId, int fromLineNum) {
        try {
            ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(executorAddress);
            Result<LogResult> logResult = executorBiz.log(new LogParam(triggerTime, logId, fromLineNum));
            if (logResult.getContent() != null && logResult.getContent().getFromLineNum() > logResult.getContent().getToLineNum()) {
                XxlJobLog jobLog = xxlJobLogMapper.load(logId);
                if (jobLog.getHandlerCode() > 0) {
                    logResult.getContent().setEnd(true);
                }
            }
            return logResult;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new Result<LogResult>(Result.FAIL_CODE, e.getMessage());
        }
    }


    /**
     * 终止执行器端工作线程的方法
     */
    @RequestMapping("/logKill")
    @ResponseBody
    public Result<String> logKill(int id) {
        XxlJobLog log = xxlJobLogMapper.load(id);
        XxlJobInfo jobInfo = xxlJobInfoMapper.loadById(log.getJobId());
        if (jobInfo == null) {
            return new Result<String>(500, I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
        }
        if (Result.SUCCESS_CODE != log.getTriggerCode()) {
            return new Result<String>(500, I18nUtil.getString("joblog_kill_log_limit"));
        }
        Result<String> runResult = null;
        try {
            ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(log.getExecutorAddress());
            runResult = executorBiz.kill(new KillParam(jobInfo.getId()));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            runResult = new Result<String>(500, e.getMessage());
        }
        if (Result.SUCCESS_CODE == runResult.getCode()) {
            log.setHandlerCode(Result.FAIL_CODE);
            log.setHandleMsg(I18nUtil.getString("joblog_kill_log_byman") + ":" + (runResult.getMessage() != null ? runResult.getMessage() : ""));
            log.setHandleTime(new Date());
            XxlJobCompleter.updateHandleInfoAndFinish(log);
            return new Result<String>(runResult.getMessage());
        } else {
            return new Result<String>(500, runResult.getMessage());
        }
    }

    @RequestMapping("/clearLog")
    @ResponseBody
    public Result<String> clearLog(int jobGroup, int jobId, int type) {
        Date clearBeforeTime = null;
        int clearBeforeNum = 0;
        if (type == 1) {
            clearBeforeTime = DateUtil.addMonths(new Date(), -1);
        } else if (type == 2) {
            clearBeforeTime = DateUtil.addMonths(new Date(), -3);
        } else if (type == 3) {
            clearBeforeTime = DateUtil.addMonths(new Date(), -6);
        } else if (type == 4) {
            clearBeforeTime = DateUtil.addYears(new Date(), -1);
        } else if (type == 5) {
            clearBeforeNum = 1000;
        } else if (type == 6) {
            clearBeforeNum = 10000;
        } else if (type == 7) {
            clearBeforeNum = 30000;
        } else if (type == 8) {
            clearBeforeNum = 100000;
        } else if (type == 9) {
            clearBeforeNum = 0;
        } else {
            return new Result<>(Result.FAIL_CODE, I18nUtil.getString("joblog_clean_type_unvalid"));
        }
        List<Long> logIds = null;
        do {
            logIds = xxlJobLogMapper.findClearJobLog(jobGroup, jobId, clearBeforeTime, clearBeforeNum, 1000);
            if (logIds != null && logIds.size() > 0) {
                xxlJobLogMapper.clearLogs(logIds);
            }
        } while (!CollectionUtil.isEmpty(logIds));
        return Result.SUCCESS;
    }

}
