package com.atp.common.context;

/**
 * 调用者上下文。显式往下传，不用 ThreadLocal。
 */
public record CallerContext(String userId, String username, String tenantId) {
}
