package com.xiaohe.ScheduledTask.admin.core.util;

/**
 * @author : 小何
 * @Description : 字符串工具类
 * @date : 2023-08-22 17:56
 */
public class StringUtil {
    public static boolean isEmpty(String str) {
        if (str == null || str.length() == 0) {
            return true;
        }
        return false;
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    public static boolean hasText(String str) {
        if (isEmpty(str) || isEmpty(str.trim())) {
            return false;
        }
        return true;
    }
}
