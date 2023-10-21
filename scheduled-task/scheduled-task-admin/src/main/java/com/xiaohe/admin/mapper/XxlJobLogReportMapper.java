package com.xiaohe.admin.mapper;

import com.xiaohe.admin.core.model.XxlJobLogReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-10-15 17:54
 */
@Mapper
public interface XxlJobLogReportMapper {

    public int update(XxlJobLogReport xxlJobLogReport);

    int save(XxlJobLogReport xxlJobLogReport);

    public XxlJobLogReport queryLogReportTotal();

    public List<XxlJobLogReport> queryLogReport(@Param("triggerDayFrom") Date triggerDayFrom,
                                                @Param("triggerDayTo") Date triggerDayTo);
}
