package com.aisale.backend.exception.business;

import com.aisale.backend.exception.BusinessException;
import com.aisale.backend.exception.ErrorCode;

/**
 * 未授权异常
 * 用于用户未登录或 Token 无效的情况
 */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException() {
        super(ErrorCode.UNAUTHORIZED);
    }

    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }

    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode);
    }

    public UnauthorizedException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

}
