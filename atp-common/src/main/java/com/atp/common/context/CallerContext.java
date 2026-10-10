package com.atp.common.context;

/**
 * 调用者上下文。显式往下传，不用 ThreadLocal。
 *
 * 项目一的教训：工具执行在另一套线程上，ThreadLocal 取不到值，
 * 而且线程池复用还可能读到别人的值。所以这里只是一个普通载体。
 */
public record CallerContext(Long userId, String username, String traceId) {
}
