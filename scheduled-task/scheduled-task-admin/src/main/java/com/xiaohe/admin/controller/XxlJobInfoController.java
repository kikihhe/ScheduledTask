package com.xiaohe.admin.controller;

import com.xiaohe.admin.core.model.XxlJobGroup;
import com.xiaohe.admin.core.model.XxlJobInfo;
import com.xiaohe.admin.core.model.XxlJobUser;
import com.xiaohe.admin.mapper.XxlJobGroupMapper;
import com.xiaohe.admin.service.LoginService;
import com.xiaohe.admin.service.XxlJobService;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-09-23 16:29
 */
@Controller
@RequestMapping("/jobinfo")
public class XxlJobInfoController {

    private static Logger logger = LoggerFactory.getLogger(XxlJobInfoController.class);
    @Resource
    private XxlJobService xxlJobService;

    @Resource
    private XxlJobGroupMapper xxlJobGroupMapper;

    @RequestMapping
    public String index(HttpServletRequest request, Model model,
                        @RequestParam(required = false, defaultValue = "-1") int jobGroup) {
        // TODO 添加一些其他数据，比如阻塞策略、路由策略...
        List<XxlJobGroup> jobGroupListAll = xxlJobGroupMapper.findAll();
        List<XxlJobGroup> jobGroupList = filterJobGroupByRole(request, jobGroupListAll);
        if (CollectionUtil.isEmpty(jobGroupList)) {
            throw new RuntimeException("jobgroup_empty");
        }
        model.addAttribute("JobGroupList", jobGroupList);
        model.addAttribute("jobGroup", jobGroup);
        return "jobinfo/jobinfo.index";

    }

    /**
     * 分页查询定时任务
     *
     * @param start
     * @param length
     * @param jobGroup
     * @param triggerStatus
     * @param jobDesc
     * @param executorHandler
     * @param author
     */
    @RequestMapping("/pageList")
    @ResponseBody
    public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        int jobGroup,
                                        int triggerStatus,
                                        String jobDesc,
                                        String executorHandler,
                                        String author) {
        return xxlJobService.pageList(start, length, jobGroup, triggerStatus, jobDesc, executorHandler, author);
    }

    @RequestMapping("/add")
    @ResponseBody
    public Result add(XxlJobInfo jobInfo) {
        return xxlJobService.add(jobInfo);
    }


    @RequestMapping("/update")
    @ResponseBody
    public Result<String> update(XxlJobInfo jobInfo) {
        return xxlJobService.update(jobInfo);
    }

    @RequestMapping("/remove")
    @ResponseBody
    public Result remove(Integer id) {
        return xxlJobService.remove(id);
    }


    /**
     * 根据角色过滤掉他管不到的执行器组
     *
     * @param request
     * @param jobGroupListAll
     */
    public static List<XxlJobGroup> filterJobGroupByRole(HttpServletRequest request, List<XxlJobGroup> jobGroupListAll) {
        List<XxlJobGroup> jobGroupList = new ArrayList<>();
        if (CollectionUtil.isEmpty(jobGroupListAll)) {
            return jobGroupList;
        }
        XxlJobUser loginUser = (XxlJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
        if (loginUser.getRole() == 1) {
            jobGroupList = jobGroupListAll;
        } else {
            ArrayList<String> permissionGroups = new ArrayList<>(Arrays.asList(loginUser.getPermission().split(",")));
            for (XxlJobGroup group : jobGroupListAll) {
                if (permissionGroups.contains(group.getId())) {
                    jobGroupList.add(group);
                }
            }
        }
        return jobGroupList;
    }

    /**
     * 校验当前登录用户是否有操作某个执行器组的权限
     *
     * @param request
     * @param jobGroup
     */
    public static void validPermission(HttpServletRequest request, int jobGroup) {
        XxlJobUser user = (XxlJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
        if (!user.validPermission(jobGroup)) {
            throw new RuntimeException("system_permission_limit, [username" + user.getUsername() + "]");
        }
    }
}
