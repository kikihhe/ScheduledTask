package com.xiaohe.ScheduledTask.core.enums;

public class RegistryConfig {

    /**
     * 执行器注册到调度中心的心跳时间，其实就是每30秒重新注册一次，刷新注册时间，以防调度中心以为执行器下线了
     */
    public static final int BEAT_TIMEOUT = 30;
    /**
     * 如果三次心跳检测都没有收到回应，默认执行器已经下线
     */
    public static final int DEAD_TIMEOUT = BEAT_TIMEOUT * 3;

    //注册类型
    public enum RegistType{ EXECUTOR, ADMIN }

}