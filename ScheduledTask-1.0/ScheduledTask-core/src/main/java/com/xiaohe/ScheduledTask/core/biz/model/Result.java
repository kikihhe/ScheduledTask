package com.xiaohe.ScheduledTask.core.biz.model;

import java.io.Serializable;

/**
 * @author : 小何
 * @Description : 统一返回结果
 * @date : 2023-08-22 22:15
 */
public class Result<T> implements Serializable {
    public static final long serialVersionUID = 42L;

    /**
     * 成功响应码
     */
    public static final int SUCCESS_CODE = 200;
    /**
     * 失败响应码
     */
    public static final int FAIL_CODE = 500;

    /**
     * 成功响应结果, code=200, message=null, content=null
     */
    public static final Result<String> SUCCESS = new Result<>(null);
    /**
     * 失败相应结果, code=500, message=null, content=null
     */
    public static final Result<String> FAIL = new Result<>(FAIL_CODE, null);

    /**
     * 响应码
     */
    private int code;

    /**
     * 相应信息
     */
    private String message;

    /**
     * 相应内容
     */
    private T content;

    public Result() {
    }

    public Result(T content) {
        this.code = SUCCESS_CODE;
        this.content = content;
    }

    public Result(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getContent() {
        return content;
    }

    public void setContent(T content) {
        this.content = content;
    }
    @Override
    public String toString() {
        return "ReturnT [code=" + code + ", message=" + message + ", content=" + content + "]";
    }

}
