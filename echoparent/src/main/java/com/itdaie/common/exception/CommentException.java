package com.itdaie.common.exception;

/**
 * 评论模块业务异常
 * 继承BusinessException，保持异常处理一致性
 *
 * @author itdaie
 */
public class CommentException extends BusinessException {

    public CommentException(String message) {
        super(message);
    }

    public CommentException(String message, Throwable cause) {
        super(message);
        this.initCause(cause);
    }
}
