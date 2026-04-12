package com.aisale.backend.exception;

/**
 * 系统异常基类
 * 用于系统级别的异常情况（如数据库错误、外部服务错误等）
 */
public class SystemException extends BaseException {

    public SystemException(String errorCode, String message, int httpStatus) {
        super(errorCode, message, httpStatus);
    }

    public SystemException(String errorCode, String message, int httpStatus, Throwable cause) {
        super(errorCode, message, httpStatus, cause);
    }

    public SystemException(ErrorCode errorCode) {
        super(errorCode);
    }

    public SystemException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public SystemException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public SystemException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode.getCode(), message, errorCode.getHttpStatus(), cause);
    }

}
