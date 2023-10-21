package com.xiaohe.admin.service;

import com.xiaohe.admin.core.model.XxlJobInfo;
import com.xiaohe.core.model.Result;

import java.util.Date;
import java.util.Map;

/**
 * @author : 小何
 * @Description : service顶级接口
 * @date : 2023-09-27 13:45
 */
public interface XxlJobService {

    public Map<String, Object> pageList(int start, int length, int jobGroup, int triggerStatus, String jobDesc, String executorHandler, String author);

    public Result add(XxlJobInfo jobInfo);

    Result<String> update(XxlJobInfo jobInfo);

    Result remove(Integer id);

    public Map<String,Object> dashboardInfo();

    public Result<Map<String,Object>> chartInfo(Date startDate, Date endDate);

    public Result<String> start(int id);
}
