package com.aisale.backend.exception;

import lombok.Getter;

/**
 * 项目异常基类
 * 所有自定义异常都必须继承此类
 */
@Getter
public class BaseException extends RuntimeException {

    /**
     * 错误码
     */
    private final String errorCode;

    /**
     * HTTP 状态码
     */
    private final int httpStatus;

    /**
     * 构造方法
     *
     * @param errorCode  错误码
     * @param message    错误消息
     * @param httpStatus HTTP 状态码
     */
    public BaseException(String errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    /**
     * 构造方法（带原始异常）
     *
     * @param errorCode  错误码
     * @param message    错误消息
     * @param httpStatus HTTP 状态码
     * @param cause      原始异常
     */
    public BaseException(String errorCode, String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    /**
     * 从 ErrorCode 枚举构造
     *
     * @param errorCode 错误码枚举
     */
    public BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode.getCode();
        this.httpStatus = errorCode.getHttpStatus();
    }

    /**
     * 从 ErrorCode 枚举构造（带原始异常）
     *
     * @param errorCode 错误码枚举
     * @param cause     原始异常
     */
    public BaseException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode.getCode();
        this.httpStatus = errorCode.getHttpStatus();
    }

    /**
     * 从 ErrorCode 枚举构造（带自定义消息）
     *
     * @param errorCode 错误码枚举
     * @param message   自定义消息
     */
    public BaseException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode.getCode();
        this.httpStatus = errorCode.getHttpStatus();
    }

}
