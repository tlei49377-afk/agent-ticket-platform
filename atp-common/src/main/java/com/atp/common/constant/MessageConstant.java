package com.atp.common.constant;

/**
 * 提示文案常量，统一管理。
 */
public class MessageConstant {
    public static final String LOGIN_FAILED = "用户名或密码错误";
    public static final String ACCOUNT_DISABLED = "账号已被禁用";
    public static final String NOT_LOGIN = "未登录或登录已过期";
    public static final String NO_PERMISSION = "你没有执行该操作的权限";
    public static final String SYSTEM_ERROR = "系统繁忙，请稍后再试";

    public static final String TICKET_NOT_FOUND = "工单不存在";
    public static final String TICKET_STATUS_ILLEGAL = "工单状态不允许该操作";

    public static final String TOOL_NOT_FOUND = "工具不存在";
    public static final String TOOL_NOT_AUTHORIZED = "该 Agent 未被授权使用此工具";
    public static final String TOOL_TIMEOUT = "操作超时，请稍后再试";

    public static final String SESSION_NOT_OWNED = "该会话不属于当前用户";

    // 私有构造，禁止new
    private MessageConstant() {
    }
}
