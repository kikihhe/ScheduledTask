package com.xiaohe.admin.controller;

import com.xiaohe.admin.core.model.XxlJobGroup;
import com.xiaohe.admin.core.model.XxlJobInfo;
import com.xiaohe.admin.core.model.XxlJobLog;
import com.xiaohe.admin.mapper.XxlJobGroupMapper;
import com.xiaohe.admin.mapper.XxlJobInfoMapper;
import com.xiaohe.admin.mapper.XxlJobLogMapper;
import com.xiaohe.core.model.Result;
import com.xiaohe.core.util.DateUtil;
import com.xiaohe.core.util.StringUtil;
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

/**
 * @author : 小何
 * @Description : 调度日志界面
 * @date : 2023-09-23 13:16
 */
@Controller
@RequestMapping("/joblog")
public class XxlJobLogController {
    private static final Logger logger = LoggerFactory.getLogger(XxlJobLogController.class);

    @Resource
    private XxlJobLogMapper xxlJobLogMapper;

    @Resource
    private XxlJobInfoMapper xxlJobInfoMapper;

    @Resource
    private XxlJobGroupMapper xxlJobGroupMapper;

    @RequestMapping
    public String index(HttpServletRequest request, Model model,
                        @RequestParam(required = false, defaultValue = "0") Integer jobId) {
        List<XxlJobGroup> jobGroupListAll = xxlJobGroupMapper.findAll();
        List<XxlJobGroup> jobGroupList = XxlJobInfoController.filterJobGroupByRole(request, jobGroupListAll);
        model.addAttribute("jobGroupList", jobGroupList);
        if (jobId > 0) {
            XxlJobInfo xxlJobInfo = xxlJobInfoMapper.loadById(jobId);
            if (xxlJobInfo == null) {
                throw new RuntimeException("jobinfo_field_id system_unvalid");
            }
            model.addAttribute("jobInfo", xxlJobInfo);
            XxlJobInfoController.validPermission(request, xxlJobInfo.getJobGroup());
        }
        return "joblog/joblog.index";
    }

    /**
     * 查看执行器组负责的所有定时任务
     * @param jobGroup 执行器组id
     * @return
     */
    @RequestMapping("/getJobsByGroup")
    public Result<List<XxlJobInfo>> getJobsByGroup(int jobGroup) {
        List<XxlJobInfo> jobsByJobGroup = xxlJobInfoMapper.getJobsByJobGroup(jobGroup);
        return Result.success("success", jobsByJobGroup);
    }

    /**
     * 根据jobGroup和jobId查询日志，可以指定调度时间。
     * @param request
     * @param start
     * @param length
     * @param jobGroup
     * @param jobId
     * @param logStatus   <br></br>
     * 1: 调度成功并且执行成功
     * 2: 调度失败或者执行失败
     * 3: 调度成功，还未开始执行
     * @param filterTime 给查询的日志指定调度日期范围
     * @return
     */
    public Map<String, Object> pageList(HttpServletRequest request,
                                        @RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        int jobGroup,
                                        int jobId,
                                        int logStatus,
                                        String filterTime) {
        XxlJobInfoController.validPermission(request, jobGroup);
        // 如果指定了调度时间，就要切割 filterTime
        Date triggerTimeStart = null;
        Date triggerTimeEnd = null;
        if (StringUtil.hasText(filterTime)) {
            String[] temp = filterTime.split(" - ");
            if (temp.length == 2) {
                triggerTimeStart = DateUtil.parseDateTime(temp[0]);
                triggerTimeEnd = DateUtil.parseDateTime(temp[1]);
            }
        }
        List<XxlJobLog> xxlJobLogs = xxlJobLogMapper.pageList(start, length, jobGroup, jobId, triggerTimeStart, triggerTimeEnd, logStatus);
        Map<String, Object> maps = new HashMap<>();
        maps.put("recordsTotal", xxlJobLogs.size());
        maps.put("recordsFilteres", xxlJobLogs.size());
        maps.put("data", xxlJobLogs);
        return maps;
    }

    /**
     * 根据日志id查看详细信息
     * @param id
     * @param model
     * @return
     */
    @RequestMapping("/logDetailPage")
    public String logDetailPage(int id, Model model) {
        XxlJobLog xxlJobLog = xxlJobLogMapper.loadById(id);
        if (xxlJobLog == null) {
            throw new RuntimeException("joblog_jogId_unvalid");
        }
        model.addAttribute("triggerCode", xxlJobLog.getTriggerCode());
        model.addAttribute("handleCode", xxlJobLog.getHandlerCode());
        model.addAttribute("executorAddress", xxlJobLog.getExecutorAddress());
        model.addAttribute("triggerTime", xxlJobLog.getTriggerTime());
        model.addAttribute("logId", xxlJobLog.getId());
        return "joblog/joblog.detail";
    }

    /**
     * 查看某个日志在具体的执行器中具体的日志信息
     * @param executorAddress
     * @param triggerTime
     * @param logId
     * @param fromLineNum
     * @return
     */
    @ResponseBody
    @RequestMapping("/logDetailCat")
    public Result logDetailCat(String executorAddress, long triggerTime, long logId, int fromLineNum) {
        // TODO 查看某个日志在具体的执行器中具体的日志信息
        return null;
    }

    /**
     * 终止执行器端工作线程的方法
     * @param id
     * @return
     */
    @ResponseBody
    @RequestMapping("/logKill")
    public Result logKill(int id) {
        // TODO 使用日志id终止对应定时任务的工作线程
        return null;
    }


    /**
     * 清除数据库中的日志, 可以指定日期、删除条数
     * @param jobGroup
     * @param jobId
     * @param type
     * @return
     */
    public Result clearLog(int jobGroup, int jobId, int type) {
        // 按天数清理
        Date clearBeforeTime = null;
        // 按日志数量清理(这个变量是指删掉这个数量之前的所有日志，即保留clearBeforeNum条日志)
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
            return Result.error("joblog_clean_type_unvalid");
        }
        List<Long> logIds = null;
        while (!(logIds = xxlJobLogMapper.findClearJobLog(jobGroup, jobId, clearBeforeTime, clearBeforeNum, 1000)).isEmpty()) {
            xxlJobLogMapper.clearLogs(logIds);
        }
        return Result.success();
    }


}
