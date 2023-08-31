package com.xiaohe.core.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author : 小何
 * @Description : cookie工具类
 * @date : 2023-08-31 15:07
 */
public class CookieUtil {

    /**
     * 默认cookie存活时间
     */
    private static final int COOKIE_MAX_AGE = Integer.MAX_VALUE;

    public static final String COOKIE_PATH = "/";

    /**
     * 将cookie 存入 response
     * @param response
     * @param key
     * @param value
     * @param ifRemember
     */
    public static void set(HttpServletResponse response,
                           String key,
                           String value,
                           boolean ifRemember) {
        int age = ifRemember ? COOKIE_MAX_AGE : -1;
        set(response, key, value, null, COOKIE_PATH, age, true);
    }

    /**
     * 将cookie 存入 response
     * @param response 响应
     * @param key
     * @param value
     * @param domain 该cookie属于哪个站点
     * @param path 路径
     * @param maxAge cookie存活时间
     * @param isHttpOnly 仅http协议可使用
     */
    private static void set(HttpServletResponse response,
                            String key,
                            String value,
                            String domain,
                            String path,
                            int maxAge,
                            boolean isHttpOnly) {
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
     * 根据 key获取 value
     * @param request
     * @param key
     * @return
     */
    public static String getValue(HttpServletRequest request, String key) {
        Cookie cookie = get(request, key);
        if (cookie != null) {
            return cookie.getValue();
        }
        return null;
    }


    private static Cookie get(HttpServletRequest request, String key) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(key)) {
                    return cookie;
                }
            }
        }
        return null;
    }

    /**
     * 从response中删除指定cookie
     * @param request
     * @param response
     * @param key
     */
    public static void remove(HttpServletRequest request, HttpServletResponse response, String key) {
        Cookie cookie = get(request, key);
        if (cookie != null) {
            set(response, key, "", null, COOKIE_PATH, 0, true);
        }
    }

}
