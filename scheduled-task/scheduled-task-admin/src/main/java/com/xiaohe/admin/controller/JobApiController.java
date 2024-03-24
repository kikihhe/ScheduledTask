package com.xiaohe.admin.controller;

import com.xiaohe.admin.core.conf.ScheduleTaskAdminConfig;
import com.xiaohe.core.biz.AdminBiz;
import com.xiaohe.core.model.HandlerCallbackParam;
import com.xiaohe.core.model.RegistryParam;
import com.xiaohe.core.model.Result;
import com.xiaohe.core.util.JsonUtil;
import com.xiaohe.core.util.StringUtil;
import com.xiaohe.core.util.XxlJobRemotingUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author : 小何
 * @Description : 调度中心的服务端，接收执行器发送的消息并处理
 * @date : 2023-10-18 19:41
 */
@Controller
@RequestMapping("/api")
public class JobApiController {
    @Resource
    private AdminBiz adminBiz;
    public Result<String> api(HttpServletRequest request, @PathVariable("uri") String uri, @RequestBody(required = false) String data) {
        if (StringUtil.hasText(uri)) {
            return Result.error("invalid request, uri-mapping empty");
        }
        String requestToken = request.getHeader(XxlJobRemotingUtil.XXL_JOB_ACCESS_TOKEN);
        if (!StringUtil.hasText(requestToken) || !requestToken.equals(ScheduleTaskAdminConfig.getAdminConfig().getAccessToken())) {
            return Result.error("the access token is wrong.");
        }
        // 开始处理任务
        if ("callback".equals(uri)) {
            List<HandlerCallbackParam> list = JsonUtil.readValue(data, List.class, HandlerCallbackParam.class);
            return adminBiz.callback(list);
        } else if ("registry".equals(uri)) {
            RegistryParam registryParam = JsonUtil.readValue(data, RegistryParam.class);
            return adminBiz.registry(registryParam);
        } else if ("registryRemove".equals(uri)) {
            RegistryParam registryParam = JsonUtil.readValue(data, RegistryParam.class);
            return adminBiz.registryRemove(registryParam);
        } else {
            return Result.error("invalid request, uri-mapping(" + uri + ") not found.");
        }
    }
}
