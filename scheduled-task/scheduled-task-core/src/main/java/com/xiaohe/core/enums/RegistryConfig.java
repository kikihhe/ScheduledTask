package com.xiaohe.core.enums;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-10-02 12:53
 */
public class RegistryConfig {
    public static final int BEAT_TIMEOUT = 30;

    public static final int DEAD_TIMEOUT = BEAT_TIMEOUT * 3;

    public enum RegistType {
        EXECUTOR,
        ADMIN
    }
}
