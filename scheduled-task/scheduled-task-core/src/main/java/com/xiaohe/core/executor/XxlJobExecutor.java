package com.xiaohe.core.executor;

import com.xiaohe.core.biz.AdminBiz;
import com.xiaohe.core.biz.client.AdminBizClient;
import com.xiaohe.core.handler.IJobHandler;
import com.xiaohe.core.handler.annotation.XxlJob;
import com.xiaohe.core.handler.impl.MethodJobHandler;
import com.xiaohe.core.log.XxlJobFileAppender;
import com.xiaohe.core.server.EmbedServer;
import com.xiaohe.core.thread.JobLogFileCleanThread;
import com.xiaohe.core.thread.JobThread;
import com.xiaohe.core.thread.TriggerCallbackThread;
import com.xiaohe.core.util.IPUtil;
import com.xiaohe.core.util.NetUtil;
import com.xiaohe.core.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : 小何
 * @Description : 执行器核心类
 * @date : 2023-09-28 20:57
 */
public class XxlJobExecutor {
    private static final Logger logger = LoggerFactory.getLogger(XxlJobExecutor.class);

    /**
     * 调度中心地址，以逗号隔开.
     * 执行器需要与调度中心通信
     */
    private String adminAddresses;

    /**
     * 通信需要使用到的token
     */
    private String accessToken;

    /**
     * 执行器的名称，执行器注册到调度中心时使用它
     */
    private String appname;

    /**
     * 执行器的IP地址
     */
    private String address;

    /**
     * 执行器端口
     */
    private int port;

    /**
     * 用户指定的日志文件地址
     */
    private String logPath;

    /**
     * 日志最大保存时间
     */
    private int logRetentionDays;


    /**
     * 执行器的启动方法
     */
    public void start() throws Exception {
        // 指定日志文件的存放位置
        XxlJobFileAppender.initLogPath(logPath);

        // 初始化执行器给调度中心发送消息的组件
        initAdminBizList(adminAddresses, accessToken);

        // 定时清理过期日志，一天一次
        JobLogFileCleanThread.getInstance().start(logRetentionDays);

        // 启动回调执行结果给调度中心的组件
        TriggerCallbackThread.getInstance().start();

    }


    // --------------------------------调度中心客户端-----------------------------------------------------
    /**
     * 执行器给调度中心发送消息的组件
     */
    private static List<AdminBiz> adminBizList;


    /**
     * 初始化执行器给调度中心发送消息的组件
     *
     * @param adminAddresses
     * @param accessToken
     */
    public void initAdminBizList(String adminAddresses, String accessToken) {
        if (!StringUtil.hasText(adminAddresses)) {
            return;
        }
        for (String address : adminAddresses.split(",")) {
            if (!StringUtil.hasText(address)) {
                continue;
            }
            AdminBizClient adminBiz = new AdminBizClient(address, accessToken);
            if (adminBizList == null) {
                adminBizList = new ArrayList<>();
            }
            adminBizList.add(adminBiz);
        }
    }

    // --------------------------------JobThread-----------------------------------------------------

    /**
     * 存放 JobThread线程 的Map
     */
    private static ConcurrentHashMap<Integer, JobThread> jobThreadRepository = new ConcurrentHashMap<>();

    /**
     * 根据任务id获取对应线程
     *
     * @param jobId
     */
    public static JobThread loadJobThread(int jobId) {
        return jobThreadRepository.get(jobId);
    }

    /**
     * 注册新任务
     *
     * @param jobId
     * @param handler
     * @param removeOldReason
     */
    public static JobThread registJobThread(int jobId, IJobHandler handler, String removeOldReason) {
        JobThread newJobThread = new JobThread(jobId, handler);
        newJobThread.start();
        logger.info(">>>>>>>>>> xxl-job regist JobThread success, jobId:{}, handler:{}", jobId, handler);
        JobThread oldJobThread = jobThreadRepository.put(jobId, newJobThread);
        // 如果任务之前注册过，可能是web端更改了任务。将旧线程停止。
        if (oldJobThread != null) {
            oldJobThread.toStop(removeOldReason);
            oldJobThread.interrupt();
        }
        return newJobThread;
    }

    /**
     * 销毁线程
     *
     * @param jobId
     * @param removeReason
     */
    public static JobThread removeJobThread(int jobId, String removeReason) {
        JobThread oldJobThread = jobThreadRepository.remove(jobId);
        if (oldJobThread != null) {
            oldJobThread.toStop(removeReason);
            oldJobThread.interrupt();
        }
        return oldJobThread;
    }

    // --------------------------------JobHandler-----------------------------------------------------
    /**
     * 存放定时任务的map
     * key: 定时任务名称
     * value: 封装的class method
     */
    private static ConcurrentHashMap<String, IJobHandler> jobHandlerRepository = new ConcurrentHashMap<>();

    public static IJobHandler loadJobHandler(String name) {
        return jobHandlerRepository.get(name);
    }

    /**
     * 注册
     *
     * @param name
     * @param jobHandler
     */
    public static IJobHandler registJobHandler(String name, IJobHandler jobHandler) {
        logger.info(">>>>>>>>>>>>>>>> xxl-job register jobHandler success, name:{}, jobHandler:{}", name, jobHandler);
        return jobHandlerRepository.put(name, jobHandler);
    }

    /**
     * 供子类调用的注册方法
     *
     * @param xxlJob
     * @param bean
     * @param method
     */
    protected void registJobHandler(XxlJob xxlJob, Object bean, Method method) throws NoSuchMethodException {
        if (xxlJob == null) {
            return;
        }
        // 获取任务名称、class、method
        String name = xxlJob.value();
        Class<?> clazz = bean.getClass();
        String methodName = method.getName();
        if (!StringUtil.hasText(name)) {
            throw new RuntimeException("xxl-job method-jobhandler name invalid, for[" + clazz + "#" + methodName + "].");
        }
        // 如果已经注册过
        if (loadJobHandler(name) != null) {
            throw new RuntimeException("xxl-job method-jobhandler[" + name + "] naming conflicts");
        }
        method.setAccessible(true);
        // 拿到初始化方法和销毁方法
        Method initMethod = null;
        Method destroyMethod = null;
        getInitAndDestroy(initMethod, destroyMethod, xxlJob, clazz);
        // 得到所有后注册入Map
        registJobHandler(name, new MethodJobHandler(bean, method, initMethod, destroyMethod));
    }

    private void getInitAndDestroy(Method initMethod, Method destoryMethod, XxlJob xxlJob, Class clazz) throws NoSuchMethodException {
        String init = xxlJob.initMethod();
        String destroy = xxlJob.destroyMethod();
        if (StringUtil.hasText(init)) {
            initMethod = clazz.getDeclaredMethod(init);
            initMethod.setAccessible(true);
        }
        if (StringUtil.hasText(destroy)) {
            destoryMethod = clazz.getDeclaredMethod(destroy);
            destoryMethod.setAccessible(true);
        }
    }

    // ---------------------------------EmbedServer-------------------------------------
    /**
     * 执行器端的服务器
     */
    private EmbedServer embedServer;

    private void initEmbedServer(String address, String ip, int port, String appname, String accessToken) throws Exception {
        port = port > 0 ? port : NetUtil.findAvailablePort(9999);
        ip = StringUtil.hasText(ip) ? ip : IPUtil.getIp();
        if (!StringUtil.hasText(address)) {
            String ip_port_address = IPUtil.getIpPort(ip, port);
            address = "http://{ip_port}/".replace("{ip_port}", ip_port_address);
        }
        if (!StringUtil.hasText(accessToken)) {
            logger.error(">>>>>>>>>>>>>>>>>>> xxl-job access token is empty. To ensure system security, please set the access token");
        }
        embedServer = new EmbedServer();
        embedServer.start(address, port, appname, accessToken);
    }

    // ------------------------------------------------------------------------------------------
    // 这些setter都是用户在config中配置的，毕竟用户需要指定日志文件放在哪里。
    public void setAdminAddresses(String adminAddresses) {
        this.adminAddresses = adminAddresses;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public void setLogRetentionDays(int logRetentionDays) {
        this.logRetentionDays = logRetentionDays;
    }

    public static List<AdminBiz> getAdminBizList() {
        return adminBizList;
    }

    public static void setAdminBizList(List<AdminBiz> adminBizList) {
        XxlJobExecutor.adminBizList = adminBizList;
    }
}
