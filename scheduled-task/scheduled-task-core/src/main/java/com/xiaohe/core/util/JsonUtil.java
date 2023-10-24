package com.xiaohe.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * @author : 小何
 * @Description : json工具(使用了Gson包)
 * @date : 2023-09-20 18:25
 */
public class JsonUtil {

    private static Logger logger = LoggerFactory.getLogger(JsonUtil.class);
    private static Gson gson = null;

    static {
        gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
    }

    public static <T> T readValue(String json, Class<T> type) {
        return gson.fromJson(json, type);
    }

    public static <T> String writeValueAsString(T object) {
        try {
            return new ObjectMapper().writeValueAsString(object);

        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return null;
    }


    public static <T> T readValue(String json, Class<T> classOfT, Class argClassOfT) {
        Type type = new ParameterizedType4ReturnT(classOfT, new Class[]{argClassOfT});
        return gson.fromJson(json, type);
    }

    public static class ParameterizedType4ReturnT implements ParameterizedType {
        private final Class raw;
        private final Type[] args;

        public ParameterizedType4ReturnT(Class raw, Type[] args) {
            this.raw = raw;
            this.args = args != null ? args : new Type[0];
        }

        @Override
        public Type[] getActualTypeArguments() {
            return args;
        }

        @Override
        public Type getRawType() {
            return raw;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    }


}
