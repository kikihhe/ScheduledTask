package com.xiaohe.admin.service;


import com.xiaohe.admin.core.model.XxlJobUser;
import com.xiaohe.admin.mapper.XxlJobUserMapper;
import com.xiaohe.util.JsonUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigInteger;


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
     * @param xxlJobUser
     * @return
     */
    private String makeToken(XxlJobUser xxlJobUser) {
        String tokenJson = JsonUtil.writeValueAsString(xxlJobUser);
        String token = new BigInteger(tokenJson.getBytes()).toString(16);
        return token;
    }

    /**
     * 将token解析为XxlJobUser类
     * @param token
     * @return
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
     * @param request
     * @param response
     * @return
     */
    public XxlJobUser ifLogin(HttpServletRequest request, HttpServletResponse response) {


        return null;
    }


}
