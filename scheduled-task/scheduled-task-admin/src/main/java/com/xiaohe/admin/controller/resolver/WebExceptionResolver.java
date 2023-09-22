package com.xiaohe.admin.controller.resolver;

import com.xiaohe.biz.model.Result;
import com.xiaohe.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author : 小何
 * @Description : 捕捉异常，
 * 如果加了ResponseBody注解，就将返回结果改为json格式，如果没有，就将 model name 改为 common/common.exception
 * @date : 2023-09-22 10:24
 */
@ControllerAdvice
public class WebExceptionResolver implements HandlerExceptionResolver {

    private static transient Logger logger = LoggerFactory.getLogger(WebExceptionResolver.class);

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        boolean isJson = false;
        if (handler instanceof HandlerMethod) {
            HandlerMethod method = (HandlerMethod) handler;
            ResponseBody responseBody = method.getMethodAnnotation(ResponseBody.class);
            if (responseBody != null) {
                isJson = true;
            }
        }
        Result result = new Result(Result.SUCCESS_CODE, ex.toString().replaceAll("\n", "</br>"));
        ModelAndView modelAndView = new ModelAndView();
        if (isJson) {
            try {
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().print(JsonUtil.writeValueAsString(request));
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
            return modelAndView;
        } else {
            modelAndView.addObject("exceptionMsg", result.getMessage());
            modelAndView.setViewName("/common/common.exception");
            return modelAndView;
        }

    }
}