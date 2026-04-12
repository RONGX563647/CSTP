package com.aisale.backend.exception.business;

import com.aisale.backend.exception.BusinessException;
import com.aisale.backend.exception.ErrorCode;

/**
 * 禁止访问异常
 * 用于用户权限不足的情况
 */
public class ForbiddenException extends BusinessException {

    public ForbiddenException() {
        super(ErrorCode.FORBIDDEN);
    }

    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, message);
    }

    public ForbiddenException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ForbiddenException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

}
