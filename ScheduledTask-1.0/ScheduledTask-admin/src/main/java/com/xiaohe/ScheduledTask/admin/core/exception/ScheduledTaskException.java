package com.xiaohe.ScheduledTask.admin.core.exception;

/**
 * @author : 小何
 * @Description : 共用异常
 * @date : 2023-08-22 15:54
 */
public class ScheduledTaskException extends RuntimeException
{

    public ScheduledTaskException() {
    }

    public ScheduledTaskException(String message) {
        super(message);
    }
}
