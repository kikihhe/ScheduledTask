package com.xiaohe.ScheduledTask.admin.core.trigger;

import com.xiaohe.ScheduledTask.admin.core.model.ScheduledTaskInfo;
import com.xiaohe.ScheduledTask.core.util.JacksonUtil;
import com.xiaohe.ScheduledTask.core.util.ObjectUtil;
import com.xiaohe.ScheduledTask.core.biz.model.Result;
import com.xiaohe.ScheduledTask.core.biz.model.TriggerParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * @author : 小何
 * @Description : 真正去执行任务的trigger
 * @date : 2023-08-23 14:21
 */
public class ScheduledTaskTrigger {
    private static Logger log = LoggerFactory.getLogger(ScheduledTaskTrigger.class);

    /**
     * 对外暴露的trigger
     * @param jobInfo
     */
    public static void trigger(ScheduledTaskInfo jobInfo) {
        processTrigger(jobInfo);

    }

    /**
     * 内部真正实现功能trigger
     * @param jobInfo
     */
    private static void processTrigger(ScheduledTaskInfo jobInfo) {
        // 初始化调用参数, 调度中心将它发送给执行器
        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setExecutorHandler(jobInfo.getExecutorHandler());
        // 选取众多执行器中的一个
        String address = jobInfo.getRegistryList().get(0);

        Result<String> triggerResult = runExecutor(triggerParam, address);
        // 这里会记录日志
        log.info("任务执行的结果: {}", triggerResult);
    }

    /**
     * 使用 HTTP 向指定IP的执行器发送调用参数
     *
     * @param triggerParam 调用参数
     * @param address      指定的执行器
     * @return 返回结果
     */
    public static Result<String> runExecutor(TriggerParam triggerParam, String address) {
        HttpURLConnection connection = null;
        BufferedReader bufferedReader = null;
        try {
            URL realURL = new URL(address);
            connection = (HttpURLConnection)realURL.openConnection();
            // 设置连接属性
            connection.setRequestMethod(HttpMethod.POST.name());
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setReadTimeout(3 * 1000);
            connection.setConnectTimeout(3 * 1000);
            // 长连接
            connection.setRequestProperty("connection", "Keep-Alive");
            // json格式的数据
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.setRequestProperty("Accept-Charset", "application/json;charset=UTF-8");

            connection.connect();
            // 如果调度参数不为空，发送给对应connection
            if (ObjectUtil.isNotNull(triggerParam)) {
                String requestBody = JacksonUtil.writeValueAsString(triggerParam);
                DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
                dataOutputStream.write(requestBody.getBytes(StandardCharsets.UTF_8));
                dataOutputStream.flush();
                dataOutputStream.close();
            }
            // 接收到响应后
            int statusCode = connection.getResponseCode();
            // 执行器执行失败
            if (statusCode != 200) {
                return new Result<String>(Result.FAIL_CODE, "ScheduledTask remoting fail, StatusCode(" + statusCode + ").");
            }
            // 执行器执行成功
            bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while (ObjectUtil.isNotNull(line = bufferedReader.readLine())) {
                result.append(line);
            }
            // 将结果转为字符串
            String resultJson = result.toString();
            return (Result<String>) JacksonUtil.readValue(resultJson, Result.class);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return new Result<String>(Result.FAIL_CODE, "ScheduledTask remoting error(" + e.getMessage() + "), for url :" + address);
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Exception e2) {
                log.error(e2.getMessage(), e2);
            }
        }
    }
}
