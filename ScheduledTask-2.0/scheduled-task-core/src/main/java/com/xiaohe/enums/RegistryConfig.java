package com.xiaohe.enums;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-09-03 14:37
 */
public class RegistryConfig {
    /**
     * 30s内没回应就算一次超时
     */
    public static final int BEAT_TIMEOUT = 30;

    /**
     * 3次超时就算这个执行器宕机
     */
    public static final int DEAD_TIMEOUT = BEAT_TIMEOUT * 3;

    public enum RegistType{ EXECUTOR, ADMIN }
}
