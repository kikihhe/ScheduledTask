package com.xiaohe.executor;

import com.xiaohe.biz.AdminBiz;
import com.xiaohe.biz.client.AdminBizClient;
import com.xiaohe.handler.IJobHandler;
import com.xiaohe.handler.annotation.ScheduledTask;
import com.xiaohe.handler.impl.MethodJobHandler;
import com.xiaohe.log.ScheduledTaskFileAppender;
import com.xiaohe.server.EmbedServer;
import com.xiaohe.thread.JobThread;
import com.xiaohe.thread.TriggerCallbackThread;
import com.xiaohe.util.IpUtil;
import com.xiaohe.util.NetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : 小何
 * @Description : 执行器的启动类
 * @date : 2023-09-02 10:45
 */
public class ScheduledTaskExecutor {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskExecutor.class);

    /**
     * 所有调度中心的地址，以逗号隔开
     */
    private String adminAddresses;

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 执行器组的名称
     */
    private String appname;

    /**
     * 执行器的地址 ip + port
     */
    private String address;

    /**
     * 执行器的IP
     */
    private String ip;

    /**
     * 执行器部署端口
     */
    private int port;

    /**
     * 日志收集路径
     */
    private String logPath;

    /**
     * 日志保留天数
     */
    private int logRetentionDays;

    // 从这里开始，就是组件重要的成员变量 <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

    /**
     * 执行器给调度中心发送消息的客户端
     */
    private static List<AdminBiz> adminBizList;

    /**
     * 执行器的服务端，用于接收调度中心发来的消息
     */
    private EmbedServer embedServer = null;

    /**
     * 存储定时任务bean的Map
     */
    private static ConcurrentHashMap<String, IJobHandler> jobHandlerRepository = new ConcurrentHashMap<>();

    /**
     * 存储执行定时任务的线程的Map
     */
    private static ConcurrentHashMap<Integer, JobThread> jobThreadRepository = new ConcurrentHashMap<>();


    /**
     * 启动执行器的各个组件               <br></br>
     * 如: 日志收集组件、执行器与调度中心通信的客户端组件、执行结果回调组件、执行器服务端组件
     */
    public void start() {

        // 初始化日志的存储路径
        ScheduledTaskFileAppender.initLogPath(logPath);

        // 初始化所有 执行器给调度中心发送消息 的客户端
        initAdminBizList(adminAddresses, accessToken);

        // 启动 将执行结果回调给调度中心 的线程
        TriggerCallbackThread.getInstance().start();

        // 启动执行器的服务端，用于接收调度中心发送的消息，如:心跳检测、忙碌检测、任务执行。
        initEmbedServer(address, ip, port, appname, accessToken);

    }

    public void destroy() throws Exception {
        // 停止内嵌服务器，再停止各个线程
        stopEmbedServer();
        // 遍历停止各个执行任务的线程
        for (Map.Entry<Integer, JobThread> entry : jobThreadRepository.entrySet()) {
            JobThread oldJobThread = removeJobThread(entry.getKey(), "wen container destroy");
            if (oldJobThread != null) {
                try {
                    oldJobThread.join();
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        jobThreadRepository.clear();
        jobHandlerRepository.clear();
        TriggerCallbackThread.getInstance().toStop();
    }


    /**
     * 初始化所有 执行器给调度中心发送消息 的客户端
     *
     * @param adminAddresses
     * @param accessToken
     */
    private void initAdminBizList(String adminAddresses, String accessToken) {
        if (adminAddresses == null && adminAddresses.trim().isEmpty()) {
            return;
        }
        String[] addressList = adminAddresses.trim().split(",");
        for (String address : addressList) {
            if (address != null && !address.trim().isEmpty()) {
                AdminBizClient adminBiz = new AdminBizClient(address.trim(), accessToken);
                if (adminBizList.isEmpty()) {
                    adminBizList = new ArrayList<>();
                }
                adminBizList.add(adminBiz);
            }
        }
    }

    /**
     * 初始化执行器端的服务器
     *
     * @param address
     * @param ip
     * @param port
     * @param appname
     * @param accessToken
     */
    public void initEmbedServer(String address, String ip, int port, String appname, String accessToken) {
        // 初始化IP、PORT、ADDRESS
        port = port > 0 ? port : NetUtil.findAvailablePort(9999);
        ip = (ip != null && !ip.trim().isEmpty()) ? ip : IpUtil.getIp();
        // 如果address为空，说明真的没有配置，那就用 ip + port 组装一个
        if (address == null || address.trim().isEmpty()) {
            String ip_port_address = IpUtil.getIpPort(ip, port);
            address = "http://{ip_port}".replace("{ip_port}", ip_port_address);
        }
        if (accessToken == null || accessToken.trim().isEmpty()) {
            logger.warn(">>>>>>>>>>> scheduled-task accessToken is empty. To ensure system security, please set the accessToken.");
        }
        embedServer = new EmbedServer();
        embedServer.start(address, port, appname, accessToken);
    }


    /**
     * 停止执行器的内嵌服务器
     */
    private void stopEmbedServer() {
        if (embedServer != null) {
            embedServer.stop();
        }
    }

    /**
     * 根据bean的名字获取定时任务bean
     *
     * @param name
     */
    public static IJobHandler loadJobHandler(String name) {
        return jobHandlerRepository.get(name);
    }

    public static IJobHandler registJobHandler(String name, IJobHandler jobHandler) {
        return jobHandlerRepository.put(name, jobHandler);
    }

    /**
     * 子类调用这个方法将某一个bean的所有加了 ScheduledTask注解 的方法注册进 jobHandlerRepository集合中
     *
     * @param method
     * @param bean
     * @param annotation 从注解中获取定时任务的名称
     */
    protected void registJobHandler(Method method, Object bean, ScheduledTask annotation) throws NoSuchMethodException {
        if (annotation == null) {
            return;
        }
        method.setAccessible(true);
        // 获取 定时任务名称、bean的class对象(用于获取初始化方法、销毁方法)、方法名称
        String name = annotation.value();
        Class<?> clazz = bean.getClass();
        String methodName = method.getName();

        // 获取该任务的初始化方法、销毁方法
        Method initMethod = null;
        Method destroyMethod = null;
        if (StringUtils.hasText(annotation.init())) {
            initMethod = clazz.getDeclaredMethod(annotation.init());
            initMethod.setAccessible(true);
        }
        if (StringUtils.hasText(annotation.destroy())) {
            destroyMethod = clazz.getDeclaredMethod(annotation.destroy());
            destroyMethod.setAccessible(true);
        }
        registJobHandler(name, new MethodJobHandler(bean, method, initMethod, destroyMethod));
    }

    /**
     * 根据定时任务的id得到执行它的线程
     *
     * @param id
     */
    public JobThread loadJobThread(Integer id) {
        return jobThreadRepository.get(id);
    }

    /**
     * 移除定时任务绑定的线程, 从Map中取出来之后调用它的 toStop方法停止
     *
     * @param jobId
     * @param removeOldReason
     */
    public static JobThread removeJobThread(int jobId, String removeOldReason) {
        JobThread oldJobThread = jobThreadRepository.remove(jobId);
        if (oldJobThread != null) {
            oldJobThread.toStop(removeOldReason);
            oldJobThread.interrupt();
        }
        return oldJobThread;
    }

    /**
     * 注册新的定时任务，为它创建新的 JobThread
     *
     * @param jobId
     * @param handler
     * @param removeOldReason
     */
    public static JobThread registJobThread(int jobId, IJobHandler handler, String removeOldReason) {
        JobThread jobThread = new JobThread(jobId, handler);
        jobThread.start();
        JobThread oldJobThread = jobThreadRepository.put(jobId, jobThread);
        // 如果oldJobThread不为空，说明Map中已经存在了执行该任务的线程
        if (oldJobThread != null) {
            oldJobThread.toStop(removeOldReason);
            oldJobThread.interrupt();
        }
        return jobThread;
    }


    public static List<AdminBiz> getAdminBizList() {
        return adminBizList;
    }

    // 下面这些set方法会在 ScheduledTaskConfig类中调用，给ScheduledTaskExecutor内的属性赋值
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

    public void setIp(String ip) {
        this.ip = ip;
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


}
