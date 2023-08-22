package com.xiaohe.ScheduledTask.admin.core.util;

import org.apache.tomcat.jni.Local;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : 小何
 * @Description : 本地缓存工具类
 * @date : 2023-08-22 17:49
 */
public class LocalCacheUtil {

    /**
     * 本地缓存类
     */
    private static class LocalCacheData {
        private String key;
        private Object value;

        private long timeoutTime;

        public LocalCacheData() {
        }

        public LocalCacheData(String key, Object value, long timeoutTime) {
            this.key = key;
            this.value = value;
            this.timeoutTime = timeoutTime;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public long getTimeoutTime() {
            return timeoutTime;
        }

        public void setTimeoutTime(long timeoutTime) {
            this.timeoutTime = timeoutTime;
        }
    }

    /**
     * 存放缓存的Map
     */
    private static ConcurrentHashMap<String, LocalCacheData> cacheRepository = new ConcurrentHashMap<>();


    public static boolean set(String key, Object val, long cacheTime) {
        // 清除缓存超时的数据
        clearTimeoutCache();
        if (!StringUtil.hasText(key)) {
            return false;
        }
        if (ObjectUtil.isNull(val) || cacheTime <= 0) {
            remove(key);
        }
        // 获取当前缓存数据的超时时间
        long timeoutTime = System.currentTimeMillis() + cacheTime;
        LocalCacheData localCacheData = new LocalCacheData(key, val, timeoutTime);
        cacheRepository.put(key, localCacheData);
        return true;
    }

    /**
     * 根据key删除缓存
     *
     * @param key
     * @return
     */
    private static boolean remove(String key) {
        if (StringUtil.hasText(key)) {
            cacheRepository.remove(key);
            return true;
        }
        return false;
    }

    /**
     * 清空过期缓存
     */
    private static boolean clearTimeoutCache() {
        if (cacheRepository.isEmpty()) {
            return true;
        }
        for (String key : cacheRepository.keySet()) {
            LocalCacheData localCacheData = cacheRepository.get(key);
            // 如果不为空且过期了，就删除
            if (ObjectUtil.isNotNull(localCacheData) && System.currentTimeMillis() >= localCacheData.getTimeoutTime()) {
                cacheRepository.remove(key);
            }
        }
        return true;
    }

    public static Object get(String key) {
        if (!StringUtil.hasText(key)) {
            return null;
        }
        LocalCacheData localCacheData = cacheRepository.get(key);
        // 如果Map中存在且没有过期
        if (!ObjectUtil.isNull(localCacheData) && System.currentTimeMillis() < localCacheData.getTimeoutTime()) {
            return localCacheData.getValue();
        } else {
            // 万一过期了，删除，返回null
            remove(key);
            return null;
        }

    }


}
