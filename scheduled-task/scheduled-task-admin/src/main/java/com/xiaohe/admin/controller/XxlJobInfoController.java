package com.xiaohe.admin.controller;

import com.xiaohe.admin.core.model.XxlJobGroup;
import com.xiaohe.admin.core.model.XxlJobUser;
import com.xiaohe.admin.service.LoginService;
import com.xiaohe.util.CollectionUtil;
import org.springframework.stereotype.Controller;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-09-23 16:29
 */
@Controller
public class XxlJobInfoController {


    /**
     * 根据角色过滤掉他管不到的执行器组
     * @param request
     * @param jobGroupListAll
     * @return
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
