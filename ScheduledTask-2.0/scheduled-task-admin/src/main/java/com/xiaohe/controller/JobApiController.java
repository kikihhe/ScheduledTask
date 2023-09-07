package com.xiaohe.controller;

import com.xiaohe.biz.AdminBiz;
import com.xiaohe.biz.model.HandleCallbackParam;
import com.xiaohe.biz.model.RegistryParam;
import com.xiaohe.biz.model.Result;
import com.xiaohe.controller.annotation.PermissionLimit;
import com.xiaohe.core.conf.ScheduledTaskAdminConfig;
import com.xiaohe.util.GsonTool;
import com.xiaohe.util.RemotingUtil;
import io.netty.handler.codec.http.HttpMethod;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 *
 * @author : 小何
 * @Description : 调度中心用于接收执行器发送的消息的服务端
 * @date : 2023-09-05 13:06
 */
@RequestMapping("/api")
@RestController
public class JobApiController {

    /**
     * AdminBizImpl, 对于从执行器发送来的消息，执行具体处理方案的是它。
     */
    @Resource
    private AdminBiz adminBiz;

    /**
     * 接收执行器发送的请求
     * @param request 请求
     * @param uri 执行器访问路径
     * @param data 执行器发送的数据，json形式
     * @return
     */
    @PermissionLimit(limit = false)
    @RequestMapping("/{uri}")
    public Result<String> api(HttpServletRequest request,
                              @PathVariable("uri") String uri,
                              @RequestBody(required = false) String data) {
        // 判断请求方式、uri、token
        String method = request.getMethod();
        String token = request.getHeader(RemotingUtil.SCHEDULED_TASK_ACCESS_TOKEN);
        if (!method.equalsIgnoreCase(HttpMethod.POST.name())) {
            return new Result<String>(Result.FAIL_CODE, "invalid request, HttpMethod not support.");
        }
        if (ScheduledTaskAdminConfig.getAdminConfig().getAccessToken().isEmpty() ||
                token.isEmpty() ||
                !ScheduledTaskAdminConfig.getAdminConfig().getAccessToken().equals(token)) {
            return new Result<>(Result.FAIL_CODE, "invalid request, access token is empty");
        }
        if (uri == null || !uri.isEmpty()) {
            return new Result<>(Result.FAIL_CODE, "invalid request, uri-mapping is not support");
        }

        // 开始判断执行器到底想干啥，然后调用具体的处理方法去执行
        if ("callback".equals(uri)) {
            List<HandleCallbackParam> callbackParamList = GsonTool.fromJson(data, List.class, HandleCallbackParam.class);
            return adminBiz.callback(callbackParamList);
        } else if ("registry".equals(uri)) {
            RegistryParam registryParam = GsonTool.fromJson(data, RegistryParam.class);
            return adminBiz.registry(registryParam);
        } else if ("registryRemove".equals(uri)) {
            RegistryParam registryParam = GsonTool.fromJson(data, RegistryParam.class);
            return adminBiz.registryRemove(registryParam);
        } else {
            // 都不匹配直接返回失败
            return new Result<>(Result.FAIL_CODE, "invalid request, uri-mapping(" + uri + ") not found");
        }
    }





}
