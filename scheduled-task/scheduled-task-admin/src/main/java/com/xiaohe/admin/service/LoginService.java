package com.xiaohe.admin.service;

import com.xiaohe.admin.core.model.XxlJobUser;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
