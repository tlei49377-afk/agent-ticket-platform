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
    public static final String USER_NOT_FOUND = "用户不存在";

    public static final String TICKET_NOT_FOUND = "工单不存在";
    public static final String TICKET_STATUS_ILLEGAL = "工单状态不允许该操作";
    public static final String TICKET_STATUS_MISSING = "状态参数缺失";
    public static final String TICKET_STATUS_SAME = "工单已处于该状态，无需重复操作";
    public static final String TICKET_CLOSED_CANNOT_REPLY = "工单已关闭，无法回复，如需继续沟通请先重开";
    public static final String TICKET_NOT_OWNED = "这不是你的工单";
    public static final String TICKET_CATEGORY_NOT_FOUND = "工单分类不存在";
    public static final String TICKET_CATEGORY_HAS_TICKET = "该分类下还有未关闭的工单，不能删除";
    public static final String PRIORITY_ILLEGAL = "优先级取值不合法";
    public static final String PRIORITY_UNCHANGED = "优先级没有变化";
    public static final String VISIBLE_ILLEGAL = "可见性只能是 public 或 internal";
    public static final String SLA_POLICY_NOT_FOUND = "该优先级的 SLA 策略不存在，请先配置";
    public static final String FILE_TYPE_NOT_ALLOWED = "不支持的文件类型";
    public static final String FILE_UPLOAD_FAILED = "文件上传失败";

    public static final String TOOL_NOT_FOUND = "工具不存在";
    public static final String TOOL_NOT_AUTHORIZED = "该 Agent 未被授权使用此工具";
    public static final String TOOL_TIMEOUT = "操作超时，请稍后再试";

    public static final String SESSION_NOT_OWNED = "该会话不属于当前用户";

    // 私有构造，禁止new
    private MessageConstant() {
    }
}
