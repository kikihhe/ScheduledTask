package com.xiaohe.util;

/**
 * @author : 小何
 * @Description : 字符串工具类
 * @date : 2023-09-20 18:37
 */
public class StringUtil {
    /**
     * 判断一个字符串是否有数据, 空格不算数据
     * @param src
     * @return
     */
    public static boolean hasText(String src) {
        return src != null && !src.isEmpty() && !src.trim().isEmpty();
    }

}
