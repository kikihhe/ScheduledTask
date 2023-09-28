package com.xiaohe.core.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author : 小何
 * @Description : json工具(使用了Gson包)
 * @date : 2023-09-20 18:25
 */
public class JsonUtil {
    private static Gson gson = null;

    static {
        gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
    }

    public static <T> T readValue(String json, Class<T> type) {
        return gson.fromJson(json, type);
    }

    public static <T> String writeValueAsString(T object) {
        return gson.toJson(object);
    }


}
