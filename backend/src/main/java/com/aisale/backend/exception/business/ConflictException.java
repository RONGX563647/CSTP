package com.aisale.backend.exception.business;

import com.aisale.backend.exception.BusinessException;
import com.aisale.backend.exception.ErrorCode;

/**
 * 资源冲突异常
 * 用于资源状态冲突、重复创建等情况
 */
public class ConflictException extends BusinessException {

    public ConflictException() {
        super(ErrorCode.RESOURCE_CONFLICT);
    }

    public ConflictException(String message) {
        super(ErrorCode.RESOURCE_CONFLICT, message);
    }

    public ConflictException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ConflictException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

}
