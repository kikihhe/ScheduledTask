package com.xiaohe.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * @author : 小何
 * @Description :
 * @date : 2023-10-23 11:10
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface XxlJobRegister {
    /**
     * 执行该定时任务的执行器组的id
     */
    int jobGroup();

    /**
     * 任务描述
     */
    String jobDesc();


    /**
     * 任务负责人
     */
    String author();

    /**
     * 报警邮件
     */
    String alarmEmail();

    /**
     * 调度类型
     */
    String scheduleType();

    /**
     * 调度的cron表达式
     */
    String scheduleConf() default "schedule_type_cron";

    /**
     * 定时任务的失败策略            <br></br>
     * 当执行器宕机，或者任务执行失败导致定时任务的执行时间没有刷新，导致超过5s的调度周期时就需要使用失败策略
     */
    String misfireStrategy() default "misfire_strategy_do_nothing";


    /**
     * 路由策略
     */
    String executorRouteStrategy();

    /**
     * JobHandler的名称
     */
    String executorHandler();

    /**
     * 定时任务执行时的参数
     */
    String executorParam() default "";

    /**
     * 定时任务的阻塞策略        <br></br>
     * 一个任务3s执行一次，但是执行一次需要耗时5s，那么就可能造成阻塞，就会使用到阻塞策略
     */
    String executorBlockStrategy();

    /**
     * 执行超时时间，单位: s
     */
    int executorTimeout() default 0;

    /**
     * 任务失败重试次数，有一个线程定时从数据库中扫描失败任务去执行
     */
    int executorFailRetryCount() default 0;


    /**
     * 子任务id        <br></br>
     * 父任务与子任务的关系: 父任务执行完毕就执行子任务，父任务执行失败就不执行子任务
     */
    String childJobId() default "";

    /**
     * 定时任务的触发状态，0:停止，1:运行
     */
    int triggerStatus() default 0;

}
