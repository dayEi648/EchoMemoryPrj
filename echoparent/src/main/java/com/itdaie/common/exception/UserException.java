package com.itdaie.common.exception;

/**
 * 用户模块业务异常。
 */
public class UserException extends BusinessException {

    public UserException(String message) {
        super(message);
    }

    public UserException(String message, Throwable cause) {
        super(message, cause);
    }
}
