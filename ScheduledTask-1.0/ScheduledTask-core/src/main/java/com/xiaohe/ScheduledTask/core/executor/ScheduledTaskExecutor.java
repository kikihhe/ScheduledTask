package com.xiaohe.ScheduledTask.core.executor;

import com.xiaohe.ScheduledTask.core.handler.IJobHandler;
import com.xiaohe.ScheduledTask.core.handler.annotation.ScheduledTask;
import com.xiaohe.ScheduledTask.core.handler.impl.MethodJobHandler;
import com.xiaohe.ScheduledTask.core.server.EmbedServer;
import com.xiaohe.ScheduledTask.core.thread.JobThread;
import com.xiaohe.ScheduledTask.core.util.IpUtil;
import com.xiaohe.ScheduledTask.core.util.NetUtil;
import com.xiaohe.ScheduledTask.core.util.ObjectUtil;
import com.xiaohe.ScheduledTask.core.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-08-24 15:15
 */
public class ScheduledTaskExecutor {
    private static Logger logger = LoggerFactory.getLogger(ScheduledTaskExecutor.class);
    private String appname;
    private String adminAddresses;

    /**
     * 存放IJobHandler的Map
     * key : 定时任务名称
     * value : JobHandler
     */
    private static ConcurrentHashMap<String, IJobHandler> jobHandlerRepository = new ConcurrentHashMap<>();

    /**
     * 存放定时任务于线程对应关系的Map
     * key : 定时任务id
     * value : 线程
     */
    private static ConcurrentHashMap<Integer, JobThread> jobThreadRepository = new ConcurrentHashMap<>();

    /**
     * 启动执行器组件
     * 1. 初始化调度中心集合
     * 2. 启动执行器服务器，使其接收消息
     */
    public void start() {
        initEmbedServer(appname, adminAddresses);

    }

    /**
     * 销毁执行器组件
     */
    public void destroy() {

    }


    /**
     * 将加了注解的方法记录
     *
     * @param annotation 注解，在其中可以得到定时任务的名字
     * @param bean       bean对象，执行时必不可少的一部分
     * @param method     方法，执行时必不可少的一部分
     */
    protected void regisJobHandler(ScheduledTask annotation, Object bean, Method method) {
        if (ObjectUtil.isNull(annotation)) {
            return;
        }
        // 定时任务名字 Class对象 方法全限定名
        String name = annotation.value();
        Class<?> clazz = bean.getClass();
        String methodName = method.getName();

        // 如果定时任务名字为空，或者名字重复
        if (!StringUtil.hasText(name)) {
            throw new RuntimeException("ScheduledTask method-jobhandler name invalid, for[" + clazz + "#" + methodName + "] .");
        }
        if (ObjectUtil.isNotNull(loadJobHandler(name))) {
            throw new RuntimeException("ScheduledTask jobhandler[" + name + "] naming conflicts.");
        }
        // 设置方法可访问
        method.setAccessible(true);
        // spring提供了容器内bean对象的管理，向外暴露了bean对象初始化方法和销毁方法的实现，所以要先执行这些方法
        Method initMethod = null;
        Method destroyMethod = null;
        // 用户指定的初始化方法名称
        if (StringUtil.hasText(annotation.init())) {
            try {
                initMethod = clazz.getDeclaredMethod(annotation.init());
                initMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
        if (StringUtil.hasText(annotation.destroy())) {
            try {
                destroyMethod = clazz.getDeclaredMethod(annotation.destroy());
                destroyMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        // 注册到Map中
        registJobHandler(name, new MethodJobHandler(bean, method, initMethod, destroyMethod));



    }

    /**
     * 在Map中查找指定名称的定时任务 IJobHandler
     *
     * @param name
     * @return
     */
    public static IJobHandler loadJobHandler(String name) {
        return jobHandlerRepository.get(name);
    }

    /**
     * 将指定定时任务注册到Map中
     *
     * @param name
     * @param jobHandler
     * @return
     */
    public static IJobHandler registJobHandler(String name, IJobHandler jobHandler) {
        logger.info(">>>>>>>>>>> ScheduledTask register jobhandler success, name:{}, jobHandler:{}", name, jobHandler);
        jobHandlerRepository.put(name, jobHandler);
        return jobHandler;
    }


    private EmbedServer embedServer = null;
    /**
     * 启动执行器内嵌的Netty服务器
     * @param appname
     * @param adminAddresses
     */
    private void initEmbedServer(String appname, String adminAddresses) {
        // 执行器默认port，后面使用配置的
        int port = NetUtil.findAvailablePort(9999);
        String ip = IpUtil.getIp();
        // 得到 IP+port
        String ip_port_address = IpUtil.getIpPort(ip, port);
        String address = "http://{ip_port}/".replace("{ip_port}", ip_port_address);
        embedServer = new EmbedServer();
        embedServer.start(address, port, appname);



    }

    /**
     * 把定时任务对应的 jobThread 缓存到Map中, 并启动该线程
     * @param jobId
     * @param handler
     * @return
     */
    public static JobThread registJobThread(int jobId, MethodJobHandler handler) {
        JobThread jobThread = new JobThread(jobId, handler);
        jobThread.start();
        jobThreadRepository.put(jobId, jobThread);
        return jobThread;
    }

    /**
     * 查询执行某个定时任务的线程
     * @param jobId 定时任务id
     * @return 负责该定时任务的线程
     */
    public static JobThread loadJobThread(int jobId) {
        return jobThreadRepository.get(jobId);
    }

}
