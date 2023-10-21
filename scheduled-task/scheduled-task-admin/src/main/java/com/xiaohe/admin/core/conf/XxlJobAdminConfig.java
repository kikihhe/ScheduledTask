package com.xiaohe.admin.core.conf;


import com.xiaohe.admin.core.alarm.JobAlarmer;
import com.xiaohe.admin.core.scheduler.XxlJobScheduler;
import com.xiaohe.admin.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;

/**
 * @author : 小何
 * @Description : 初始化调度中心的配置类, 这个类会读取用户在ynl、properties中的配置以供调用
 * @date : 2023-10-05 19:44
 */
@Component
public class XxlJobAdminConfig implements InitializingBean, DisposableBean {
    private static Logger logger = LoggerFactory.getLogger(XxlJobAdminConfig.class);

    /**
     * XxlJobAdminConfig想要配置和mapper文件，就要等spring容器初始化后再初始化
     */
    private static XxlJobAdminConfig adminConfig = null;

    /**
     * 调度中心
     */
    private XxlJobScheduler xxlJobScheduler;

    /**
     * spring容器装填完毕后, 初始化XxlJobAdminConfig, 再将调度中心启动
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        adminConfig = this;
        xxlJobScheduler = new XxlJobScheduler();
        xxlJobScheduler.start();
    }

    /**
     * spring容器销毁前将调度中心销毁
     * @throws Exception
     */
    @Override
    public void destroy() throws Exception {
        xxlJobScheduler.destroy();
    }



    // ---------------------------在配置文件中配的内容---------------------------------------------------------

    @Value("${xxl.job.accessToken}")
    private String accessToken;

    @Value("${spring.mail.from}")
    private String emailFrom;

    /**
     * 快线程池的最大线程数
     */
    @Value("${xxl.job.triggerpool.fast.max}")
    private int triggerPoolFastMax;

    /**
     * 慢线程池的最大线程数
     */
    @Value("${xxl.job.triggerpool.slow.max}")
    private int triggerPoolSlowMax;

    /**
     * 日志保留时间
     */
    @Value("${xxl.job.logretentiondays}")
    private int logretentiondays;

    // --------------------------------操作数据库的Mapper--------------------------------------------------
    @Resource
    private XxlJobGroupMapper xxlJobGroupMapper;

    @Resource
    private XxlJobInfoMapper xxlJobInfoMapper;

    @Resource
    private XxlJobLogGlueMapper xxlJobLogGlueMapper;

    @Resource
    private XxlJobRegistryMapper xxlJobRegistryMapper;

    @Resource
    private XxlJobLogMapper xxlJobLogMapper;

    @Resource
    private XxlJobUserMapper xxlJobUserMapper;

    @Resource
    private XxlJobLogReportMapper xxlJobLogReportMapper;

    @Resource
    private JavaMailSender mailSender;

    @Resource
    private JobAlarmer jobAlarmer;

    @Resource
    private DataSource dataSource;




    public static XxlJobAdminConfig getAdminConfig() {
        return adminConfig;
    }


    public static void setAdminConfig(XxlJobAdminConfig adminConfig) {
        XxlJobAdminConfig.adminConfig = adminConfig;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getEmailFrom() {
        return emailFrom;
    }

    public void setEmailFrom(String emailFrom) {
        this.emailFrom = emailFrom;
    }

    public XxlJobRegistryMapper getXxlJobRegistryMapper() {
        return xxlJobRegistryMapper;
    }

    public void setXxlJobRegistryMapper(XxlJobRegistryMapper xxlJobRegistryMapper) {
        this.xxlJobRegistryMapper = xxlJobRegistryMapper;
    }

    public int getTriggerPoolFastMax() {
        if (triggerPoolFastMax <= 200) {
            return 200;
        }
        return triggerPoolFastMax;
    }

    public void setTriggerPoolFastMax(int triggerPoolFastMax) {
        this.triggerPoolFastMax = triggerPoolFastMax;
    }

    public int getTriggerPoolSlowMax() {
        if (triggerPoolSlowMax <= 100) {
            return 100;
        }
        return triggerPoolSlowMax;
    }

    public XxlJobLogReportMapper getXxlJobLogReportMapper() {
        return xxlJobLogReportMapper;
    }

    public void setXxlJobLogReportMapper(XxlJobLogReportMapper xxlJobLogReportMapper) {
        this.xxlJobLogReportMapper = xxlJobLogReportMapper;
    }

    public void setTriggerPoolSlowMax(int triggerPoolSlowMax) {
        this.triggerPoolSlowMax = triggerPoolSlowMax;
    }

    public int getLogretentiondays() {
        return logretentiondays;
    }

    public void setLogretentiondays(int logretentiondays) {
        this.logretentiondays = logretentiondays;
    }

    public XxlJobGroupMapper getXxlJobGroupMapper() {
        return xxlJobGroupMapper;
    }

    public void setXxlJobGroupMapper(XxlJobGroupMapper xxlJobGroupMapper) {
        this.xxlJobGroupMapper = xxlJobGroupMapper;
    }

    public XxlJobInfoMapper getXxlJobInfoMapper() {
        return xxlJobInfoMapper;
    }

    public void setXxlJobInfoMapper(XxlJobInfoMapper xxlJobInfoMapper) {
        this.xxlJobInfoMapper = xxlJobInfoMapper;
    }

    public XxlJobLogGlueMapper getXxlJobLogGlueMapper() {
        return xxlJobLogGlueMapper;
    }

    public void setXxlJobLogGlueMapper(XxlJobLogGlueMapper xxlJobLogGlueMapper) {
        this.xxlJobLogGlueMapper = xxlJobLogGlueMapper;
    }

    public XxlJobLogMapper getXxlJobLogMapper() {
        return xxlJobLogMapper;
    }

    public void setXxlJobLogMapper(XxlJobLogMapper xxlJobLogMapper) {
        this.xxlJobLogMapper = xxlJobLogMapper;
    }

    public XxlJobUserMapper getXxlJobUserMapper() {
        return xxlJobUserMapper;
    }

    public void setXxlJobUserMapper(XxlJobUserMapper xxlJobUserMapper) {
        this.xxlJobUserMapper = xxlJobUserMapper;
    }

    public JavaMailSender getMailSender() {
        return mailSender;
    }

    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public JobAlarmer getJobAlarmer() {
        return jobAlarmer;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}
