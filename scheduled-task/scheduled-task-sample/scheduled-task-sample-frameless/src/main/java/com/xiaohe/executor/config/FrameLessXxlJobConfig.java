package com.xiaohe.executor.config;

import com.xiaohe.core.executor.impl.XxlJobSimpleExecutor;
import com.xiaohe.executor.jobHandler.SampleXxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Properties;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-10-21 21:11
 */
public class FrameLessXxlJobConfig {
    private static Logger logger = LoggerFactory.getLogger(FrameLessXxlJobConfig.class);


    private static FrameLessXxlJobConfig instance = new FrameLessXxlJobConfig();
    public static FrameLessXxlJobConfig getInstance() {
        return instance;
    }


    private XxlJobSimpleExecutor xxlJobExecutor = null;

    public void initXxlJobExecutor() {
        Properties xxlJobProp = loadProperties("xxl-job-executor.properties");
        xxlJobExecutor = new XxlJobSimpleExecutor();
        // 设置属性
        xxlJobExecutor.setAdminAddresses(xxlJobProp.getProperty("xxl.job.admin.addresses"));
        xxlJobExecutor.setAccessToken(xxlJobProp.getProperty("xxl.job.accessToken"));
        xxlJobExecutor.setAppname(xxlJobProp.getProperty("xxl.job.executor.appname"));
        xxlJobExecutor.setAddress(xxlJobProp.getProperty("xxl.job.executor.address"));
        xxlJobExecutor.setIp(xxlJobProp.getProperty("xxl.job.executor.ip"));
        xxlJobExecutor.setPort(Integer.valueOf(xxlJobProp.getProperty("xxl.job.executor.port")));
        xxlJobExecutor.setLogPath(xxlJobProp.getProperty("xxl.job.executor.logpath"));
        xxlJobExecutor.setLogRetentionDays(Integer.valueOf(xxlJobProp.getProperty("xxl.job.executor.logretentiondays")));

        xxlJobExecutor.setXxlJobBeanList(Arrays.asList(new SampleXxlJob()));

        // 启动执行器
        try {
            xxlJobExecutor.start();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static Properties loadProperties(String propertyFileName) {
        InputStreamReader in = null;
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            in = new InputStreamReader(loader.getResourceAsStream(propertyFileName), StandardCharsets.UTF_8);
            if (in != null) {
                Properties prop = new Properties();
                prop.load(in);
                return prop;
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }
        }
        return null;
    }
}
