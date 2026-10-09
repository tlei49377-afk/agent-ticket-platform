package com.atp.common.exception;

/**
 * 权限不足。
 */
public class PermissionDeniedException extends BaseException {
    public PermissionDeniedException(String message) {
        super(message);
    }
}
