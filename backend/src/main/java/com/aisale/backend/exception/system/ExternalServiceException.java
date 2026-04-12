package com.aisale.backend.exception.system;

import com.aisale.backend.exception.ErrorCode;
import com.aisale.backend.exception.SystemException;

/**
 * 外部服务异常
 * 用于调用外部服务失败的情况
 */
public class ExternalServiceException extends SystemException {

    public ExternalServiceException() {
        super(ErrorCode.EXTERNAL_SERVICE_ERROR);
    }

    public ExternalServiceException(String message) {
        super(ErrorCode.EXTERNAL_SERVICE_ERROR, message);
    }

    public ExternalServiceException(Throwable cause) {
        super(ErrorCode.EXTERNAL_SERVICE_ERROR, "外部服务调用失败", cause);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(ErrorCode.EXTERNAL_SERVICE_ERROR, message, cause);
    }

}
