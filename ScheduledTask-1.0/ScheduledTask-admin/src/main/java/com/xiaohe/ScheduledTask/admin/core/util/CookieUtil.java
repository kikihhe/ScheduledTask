package com.xiaohe.ScheduledTask.admin.core.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;

/**
 * @author : 小何
 * @Description : cookie工具类
 * @date : 2023-08-22 17:25
 */
public class CookieUtil {
    /**
     * cookie的最大缓存时间
     */
    private static final int COOKIE_MAX_AGE = Integer.MAX_VALUE;

    /**
     * 默认的cookie保存路径
     */
    private static final String COOKIE_PATH = "/";

    /**
     * 将cookie设置到response中
     *
     * @param response
     * @param key
     * @param value
     * @param ifRemember
     */
    public static void set(HttpServletResponse response, String key, String value, boolean ifRemember) {
        int age = ifRemember ? COOKIE_MAX_AGE : -1;
        set(response, key, value, null, COOKIE_PATH, age, true);
    }

    private static void set(HttpServletResponse response, String key, String value, String domain, String path, int maxAge, boolean isHttpOnly) {
        Cookie cookie = new Cookie(key, value);
        if (!Objects.isNull(domain)) {
            cookie.setDomain(domain);
        }
        cookie.setPath(path);
        cookie.setMaxAge(maxAge);
        cookie.setHttpOnly(isHttpOnly);

        response.addCookie(cookie);
    }

    /**
     * 从请求中查找是否有这个key
     *
     * @param request
     * @param key
     * @return
     */
    public static String getValue(HttpServletRequest request, String key) {
        Cookie cookie = get(request, key);
        if (Objects.isNull(cookie)) {
            return null;
        }
        return cookie.getValue();
    }

    private static Cookie get(HttpServletRequest request, String key) {
        Cookie[] cookies = request.getCookies();
        if (!Objects.isNull(cookies) && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(key)) {
                    return cookie;
                }
            }
        }
        return null;
    }

    /**
     * 将cookie从请求和响应中移除
     * @param request
     * @param response
     * @param key
     */
    public static void remove(HttpServletRequest request, HttpServletResponse response, String key) {
        Cookie cookie = get(request, key);
        if (!Objects.isNull(cookie)) {
            set(response, key, "", null, COOKIE_PATH, 0, true);
        }
    }


}
