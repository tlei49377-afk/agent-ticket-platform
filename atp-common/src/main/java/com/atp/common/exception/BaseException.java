package com.atp.common.exception;

/**
 * 业务异常。抛出后由全局异常处理器转成统一响应。
 */
public class BaseException extends RuntimeException{

    /**
     * @param message 异常信息
     */
    public BaseException(String message) {
        super(message);
    }

    /**
     * @param message 异常信息
     * @param cause   异常原因
     */
    public BaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
