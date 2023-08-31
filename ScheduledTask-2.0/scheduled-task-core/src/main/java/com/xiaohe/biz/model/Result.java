package com.xiaohe.biz.model;

import java.io.Serializable;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-08-31 19:48
 */
public class Result<T> implements Serializable {
    public static final long serialVersionUID = 42L;

    public static final int SUCCESS_CODE = 200;
    public static final int FAIL_CODE = 500;

    public static final Result<String> SUCCESS = new Result<>(null);
    public static final Result<String> FAIL = new Result<>(FAIL_CODE, null);

    private int code;

    private String message;

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
        return "Result [code=" + code + ", message=" + message + ", content=" + content + "]";
    }

}
