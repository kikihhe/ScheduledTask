package com.xiaohe.admin.core.alarm.impl;

import com.xiaohe.admin.core.alarm.JobAlarm;
import com.xiaohe.admin.core.conf.ScheduleTaskAdminConfig;
import com.xiaohe.admin.core.model.XxlJobGroup;
import com.xiaohe.admin.core.model.XxlJobInfo;
import com.xiaohe.admin.core.model.XxlJobLog;
import com.xiaohe.admin.core.util.I18nUtil;
import com.xiaohe.core.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.internet.MimeMessage;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


/**
 * @author : 小何
 * @Description : 通过邮件的方式告警
 * @date : 2023-10-05 20:33
 */
public class EmailJobAlarm implements JobAlarm {

    private static Logger logger = LoggerFactory.getLogger(EmailJobAlarm.class);

    //设置报警信息的发送者
    String personal = I18nUtil.getString("admin_name_full");
    //设置报警信息的标题
    String title = I18nUtil.getString("jobconf_monitor");
    @Override
    public boolean doAlarm(XxlJobInfo xxlJobInfo, XxlJobLog xxlJobLog) {
        boolean alarmResult = true;
        if (xxlJobInfo == null || xxlJobLog == null) {
            return false;
        }
        // 拼接告警内容
        String content = getContent(xxlJobInfo, xxlJobLog);
        // 拿到这个任务的负责邮箱，遍历发送给所有邮箱
        Set<String> emailSet = new HashSet<>(Arrays.asList(xxlJobInfo.getAlarmEmail().split(",")));
        for (String email : emailSet) {
            try {
                MimeMessage mimeMessage = ScheduleTaskAdminConfig.getAdminConfig().getMailSender().createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
                helper.setFrom(ScheduleTaskAdminConfig.getAdminConfig().getEmailFrom(), personal);
                helper.setTo(email);
                helper.setSubject(title);
                helper.setText(content, true);
                ScheduleTaskAdminConfig.getAdminConfig().getMailSender().send(mimeMessage);
            } catch (Exception e) {
                logger.error(">>>>>>>>>>>>>> xxl-job, job fail alarm email send error, JobLogId:{}", xxlJobLog.getId(), e);
                alarmResult = false;
            }
        }
        return alarmResult;
    }

    private String getContent(XxlJobInfo xxlJobInfo, XxlJobLog xxlJobLog) {
        // 拼接告警的内容
        String alarmContent = "Alarm Job LogId=" + xxlJobLog.getId();
        if (xxlJobLog.getTriggerCode() != Result.SUCCESS_CODE) {
            alarmContent += "<br>TriggerMsg=<br>" + xxlJobLog.getTriggerMsg();
        }
        if (xxlJobLog.getHandlerCode() > 0 && xxlJobLog.getHandlerCode() != Result.SUCCESS_CODE) {
            alarmContent += "<br>HandlerMsg=<br>" + xxlJobLog.getHandleMsg();
        }
        // 得到负责该任务的执行器组
        XxlJobGroup xxlJobGroup = ScheduleTaskAdminConfig.getAdminConfig().getXxlJobGroupMapper().load(Integer.valueOf(xxlJobInfo.getJobGroup()));
        return MessageFormat.format(loadEmailJobAlarmTemplate(),
                xxlJobGroup != null ? xxlJobGroup.getTitle() : "null",
                xxlJobInfo.getId(),
                xxlJobInfo.getJobDesc(),
                alarmContent
        );
    }


    /**
     * 邮箱模板
     */
    private static final String loadEmailJobAlarmTemplate(){
        String mailBodyTemplate = "<h5>" + I18nUtil.getString("jobconf_monitor_detail") + "：</span>" +
                "<table border=\"1\" cellpadding=\"3\" style=\"border-collapse:collapse; width:80%;\" >\n" +
                "   <thead style=\"font-weight: bold;color: #ffffff;background-color: #ff8c00;\" >" +
                "      <tr>\n" +
                "         <td width=\"20%\" >"+ I18nUtil.getString("jobinfo_field_jobgroup") +"</td>\n" +
                "         <td width=\"10%\" >"+ I18nUtil.getString("jobinfo_field_id") +"</td>\n" +
                "         <td width=\"20%\" >"+ I18nUtil.getString("jobinfo_field_jobdesc") +"</td>\n" +
                "         <td width=\"10%\" >"+ I18nUtil.getString("jobconf_monitor_alarm_title") +"</td>\n" +
                "         <td width=\"40%\" >"+ I18nUtil.getString("jobconf_monitor_alarm_content") +"</td>\n" +
                "      </tr>\n" +
                "   </thead>\n" +
                "   <tbody>\n" +
                "      <tr>\n" +
                "         <td>{0}</td>\n" +
                "         <td>{1}</td>\n" +
                "         <td>{2}</td>\n" +
                "         <td>"+ I18nUtil.getString("jobconf_monitor_alarm_type") +"</td>\n" +
                "         <td>{3}</td>\n" +
                "      </tr>\n" +
                "   </tbody>\n" +
                "</table>";
        return mailBodyTemplate;
    }

}
