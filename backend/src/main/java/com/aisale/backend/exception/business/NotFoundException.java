package com.aisale.backend.exception.business;

import com.aisale.backend.exception.BusinessException;
import com.aisale.backend.exception.ErrorCode;

/**
 * 资源不存在异常
 * 用于查询的资源不存在的情况
 */
public class NotFoundException extends BusinessException {

    public NotFoundException() {
        super(ErrorCode.RESOURCE_NOT_FOUND);
    }

    public NotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    public NotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    public NotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

}
