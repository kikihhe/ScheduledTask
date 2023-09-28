package com.xiaohe.admin.service;


import com.xiaohe.admin.core.model.XxlJobUser;
import com.xiaohe.admin.core.util.CookieUtil;
import com.xiaohe.admin.mapper.XxlJobUserMapper;
import com.xiaohe.core.model.Result;
import com.xiaohe.core.util.JsonUtil;
import com.xiaohe.core.util.StringUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;


/**
 * @author : 小何
 * @Description :
 * @date : 2023-09-21 22:44
 */
@Service
public class LoginService {
    /**
     * 用户的登录身份，用户登录之后获取用户的key, 请求头中用户的key就是它
     */
    public static final String LOGIN_IDENTITY_KEY = "XXL_JOB_LOGIN_IDENTITY";

    @Resource
    private XxlJobUserMapper xxlJobUserMapper;

    /**
     * 制作用户的token
     *
     * @param xxlJobUser
     */
    private String makeToken(XxlJobUser xxlJobUser) {
        String tokenJson = JsonUtil.writeValueAsString(xxlJobUser);
        String token = new BigInteger(tokenJson.getBytes()).toString(16);
        return token;
    }

    /**
     * 将token解析为XxlJobUser类
     *
     * @param token
     */
    private XxlJobUser parseToken(String token) {
        XxlJobUser xxlJobUser = null;
        if (token != null) {
            String tokenJson = new String(new BigInteger(token, 16).toByteArray());
            xxlJobUser = JsonUtil.readValue(tokenJson, XxlJobUser.class);
        }
        return xxlJobUser;
    }


    /**
     * 判断用户是否已经登录
     *
     * @param request
     * @param response
     */
    public XxlJobUser ifLogin(HttpServletRequest request, HttpServletResponse response) {
        String cookieToken = CookieUtil.getCookie(request, LOGIN_IDENTITY_KEY);
        if (cookieToken == null) {
            return null;
        }
        XxlJobUser user = null;
        try {
            user = parseToken(cookieToken);
        } catch (Exception e) {
            // 如果解析token的过程中出现任何异常，直接使当前用户退出登录
            logout(request, response);
        }
        if (user == null) {
            return null;
        }

        XxlJobUser dbUser = xxlJobUserMapper.loadByUsername(user.getUsername());
        if (dbUser == null || !dbUser.equals(user)) {
            return null;
        }
        return dbUser;
    }

    /**
     * 退出登录
     * @param request
     * @param response
     * @return
     */
    public Result<String> logout(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.remove(request, response, LOGIN_IDENTITY_KEY);
        return Result.success();
    }

    /**
     * 登录
     * @param request
     * @param response
     * @param username
     * @param password
     * @param ifRemember
     * @return
     */
    public Result login(HttpServletRequest request, HttpServletResponse response,
                                String username, String password, boolean ifRemember) {
        if (!StringUtil.hasText(username) || username.trim().length() < 4 || username.trim().length() > 20) {
            return Result.error("用户名不符合格式");
        }
        if (!StringUtil.hasText(password) || password.trim().length() < 4 || password.trim().length() > 20) {
            return Result.error("密码不符合格式");
        }
        XxlJobUser xxlJobUser = xxlJobUserMapper.loadByUsername(username);
        if (xxlJobUser == null) {
            return Result.error("用户名/密码错误", new XxlJobUser().setUsername(username).setPassword(password));
        }
        String passwordMd5 = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
        if (!passwordMd5.equals(xxlJobUser.getPassword())) {
            return Result.error("用户名/密码错误", new XxlJobUser().setUsername(username).setPassword(password));
        }
        // 如果登录成功，生成token，存入response返回给前端
        String token = makeToken(xxlJobUser);
        CookieUtil.set(response, LOGIN_IDENTITY_KEY, token, ifRemember);
        return Result.success();
    }


}
