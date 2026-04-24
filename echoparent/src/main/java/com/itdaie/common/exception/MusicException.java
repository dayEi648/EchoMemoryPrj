package com.itdaie.common.exception;

/**
 * 音乐模块业务异常。
 */
public class MusicException extends BusinessException {

    public MusicException(String message) {
        super(message);
    }

    public MusicException(String message, Throwable cause) {
        super(message, cause);
    }
}
