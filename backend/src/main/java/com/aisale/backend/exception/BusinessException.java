package com.aisale.backend.exception;

/**
 * 业务异常基类
 * 用于业务逻辑中可预见的异常情况
 */
public class BusinessException extends BaseException {

    public BusinessException(String errorCode, String message, int httpStatus) {
        super(errorCode, message, httpStatus);
    }

    public BusinessException(String errorCode, String message, int httpStatus, Throwable cause) {
        super(errorCode, message, httpStatus, cause);
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

}
