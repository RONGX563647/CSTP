package com.aisale.backend.exception.system;

import com.aisale.backend.exception.ErrorCode;
import com.aisale.backend.exception.SystemException;

/**
 * 数据库异常
 * 用于数据库操作失败的情况
 */
public class DatabaseException extends SystemException {

    public DatabaseException() {
        super(ErrorCode.DATABASE_ERROR);
    }

    public DatabaseException(String message) {
        super(ErrorCode.DATABASE_ERROR, message);
    }

    public DatabaseException(Throwable cause) {
        super(ErrorCode.DATABASE_ERROR, "数据库操作失败", cause);
    }

    public DatabaseException(String message, Throwable cause) {
        super(ErrorCode.DATABASE_ERROR, message, cause);
    }

}
