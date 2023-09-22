package com.xiaohe.admin.core.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-09-22 21:16
 */
public class CookieUtil {
    /**
     * Cookie的最大缓存时间
     */
    private static final int COOKIE_MAX_AGE = Integer.MAX_VALUE;

    /**
     * Cookie的存储路径
     */
    private static final String COOKIE_PATH = "/";

    /**
     * 将指定的key value 设置到 response 中
     * @param response
     * @param key
     * @param value
     * @param isRemember
     */
    public static void set(HttpServletResponse response, String key, String value, boolean isRemember) {
        int age = isRemember ? COOKIE_MAX_AGE : -1;
        set(response, key, value, null, COOKIE_PATH, age, true);
    }

    private static void set(HttpServletResponse response, String key, String value,
                            String domain, String path, int maxAge, boolean isHttpOnly) {
        Cookie cookie = new Cookie(key, value);
        if (domain != null) {
            cookie.setDomain(domain);
        }
        cookie.setPath(path);
        cookie.setMaxAge(maxAge);
        cookie.setHttpOnly(isHttpOnly);
        response.addCookie(cookie);
    }

    /**
     * 从请求中获取对应key的value
     * @param request
     * @param key
     * @return
     */
    public static String getCookie(HttpServletRequest request, String key) {
        Cookie cookie = get(request, key);
        if (cookie != null) {
            return cookie.getValue();
        }
        return null;
    }

    private static Cookie get(HttpServletRequest request, String key) {
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(key)) {
                return cookie;
            }
        }
        return null;
    }


    /**
     * 从请求和响应中将对应的cookie删除
     * @param request
     * @param response
     * @param key
     */
    public static void remove(HttpServletRequest request, HttpServletResponse response, String key) {
        if (get(request, key) != null) {
            set(response, key, null, null, COOKIE_PATH, 0, true);
        }
    }
}
