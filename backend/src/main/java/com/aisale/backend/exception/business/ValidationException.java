package com.aisale.backend.exception.business;

import com.aisale.backend.exception.BusinessException;
import com.aisale.backend.exception.ErrorCode;

/**
 * 参数校验异常
 * 用于参数验证失败的情况
 */
public class ValidationException extends BusinessException {

    public ValidationException() {
        super(ErrorCode.PARAM_VALIDATION_ERROR);
    }

    public ValidationException(String message) {
        super(ErrorCode.PARAM_VALIDATION_ERROR, message);
    }

    public ValidationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ValidationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

}
