package com.xiaohe.core.alarm.impl;

import com.xiaohe.biz.model.Result;
import com.xiaohe.core.alarm.JobAlarm;
import com.xiaohe.core.conf.ScheduledTaskAdminConfig;
import com.xiaohe.core.model.ScheduledTaskGroup;
import com.xiaohe.core.model.ScheduledTaskInfo;
import com.xiaohe.core.model.ScheduledTaskLog;
import com.xiaohe.core.util.I18nUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.ObjectUtils;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;

/**
 * @author : 小何
 * @Description : 发送邮箱告警
 * @date : 2023-09-17 12:51
 */
public class EmailJobAlarm implements JobAlarm {
    private static Logger logger = LoggerFactory.getLogger(EmailJobAlarm.class);


    @Override
    public boolean doAlarm(ScheduledTaskInfo jobInfo, ScheduledTaskLog jobLog) {
        boolean alarmResult = true;
        if (!Objects.isNull(jobInfo) && !Objects.isNull(jobLog)) {
            String alarmContent = "Alarm Job LogId=" + jobInfo.getId();
            if (jobLog.getTriggerCode() != Result.SUCCESS_CODE) {
                alarmContent += "<br>TriggerMsg=<br>" + jobLog.getTriggerMsg();
            }
            if (jobLog.getHandleCode() != Result.SUCCESS_CODE) {
                alarmContent += "<br>HandlerMsg=" + jobLog.getHandleMsg();
            }
            ScheduledTaskGroup group = ScheduledTaskAdminConfig.getAdminConfig().getScheduledTaskGroupMapper().loadById(jobInfo.getJobGroup());
            String personal = "admin_name_full";
            String title = "jobconf_monitor";
            //向模版中填充具体的内容
            String content = MessageFormat.format(loadEmailTemplate(),
                    group != null ? group.getTitle() : "null",
                    jobInfo.getId(),
                    jobInfo.getJobDesc(),
                    alarmContent
            );
            // 可能设置了多个邮件
            HashSet<String> emailSet = new HashSet<>(Arrays.asList(jobInfo.getAlarmEmail().split(",")));
            // 开始发送
            for (String email : emailSet) {
                try {
                    MimeMessage mimeMessage = ScheduledTaskAdminConfig.getAdminConfig().getMailSender().createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
                    helper.setFrom(ScheduledTaskAdminConfig.getAdminConfig().getEmailFrom());
                    helper.setTo(email);
                    helper.setSubject(title);
                    helper.setText(content, true);
                    ScheduledTaskAdminConfig.getAdminConfig().getMailSender().send(mimeMessage);
                } catch (MessagingException e) {
                    logger.error(e.getMessage(), e);
                    alarmResult = false;
                }
            }
        }


        return alarmResult;
    }


    /**
     * 加载邮箱的模板(前端)
     */
    private String loadEmailTemplate() {
        return "<h5>" + I18nUtil.getString("jobconf_monitor_detail") + "：</span>" +
                "<table border=\"1\" cellpadding=\"3\" style=\"border-collapse:collapse; width:80%;\" >\n" +
                "   <thead style=\"font-weight: bold;color: #ffffff;background-color: #ff8c00;\" >" +
                "      <tr>\n" +
                "         <td width=\"20%\" >" + I18nUtil.getString("jobinfo_field_jobgroup") + "</td>\n" +
                "         <td width=\"10%\" >" + I18nUtil.getString("jobinfo_field_id") + "</td>\n" +
                "         <td width=\"20%\" >" + I18nUtil.getString("jobinfo_field_jobdesc") + "</td>\n" +
                "         <td width=\"10%\" >" + I18nUtil.getString("jobconf_monitor_alarm_title") + "</td>\n" +
                "         <td width=\"40%\" >" + I18nUtil.getString("jobconf_monitor_alarm_content") + "</td>\n" +
                "      </tr>\n" +
                "   </thead>\n" +
                "   <tbody>\n" +
                "      <tr>\n" +
                "         <td>{0}</td>\n" +
                "         <td>{1}</td>\n" +
                "         <td>{2}</td>\n" +
                "         <td>" + I18nUtil.getString("jobconf_monitor_alarm_type") + "</td>\n" +
                "         <td>{3}</td>\n" +
                "      </tr>\n" +
                "   </tbody>\n" +
                "</table>";

    }
}
