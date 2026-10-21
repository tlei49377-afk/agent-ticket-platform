package com.atp.common.constant;

/**
 * 业务状态码常量。码值 → 中文名的翻译也放这里，避免在多个类里各写一份 switch。
 */
public class StatusConstant {

    /** 工单状态 */
    public static final int TICKET_PENDING      = 1; // 待受理
    public static final int TICKET_PROCESSING   = 2; // 处理中
    public static final int TICKET_SUSPENDED    = 3; // 已挂起
    public static final int TICKET_CONFIRMING   = 4; // 待用户确认
    public static final int TICKET_RESOLVED     = 5; // 已解决
    public static final int TICKET_CLOSED       = 6; // 已关闭

    /** 优先级 */
    public static final int PRIORITY_URGENT = 1; // 紧急
    public static final int PRIORITY_HIGH   = 2; // 高
    public static final int PRIORITY_NORMAL = 3; // 中
    public static final int PRIORITY_LOW    = 4; // 低

    /** SLA 计时器类型 */
    public static final String TIMER_RESPOND = "respond"; // 响应型
    public static final String TIMER_RESOLVE = "resolve"; // 解决型

    /** SLA 计时器状态 */
    public static final String SLA_NORMAL   = "normal";
    public static final String SLA_WARN     = "warn";
    public static final String SLA_BREACHED = "breached";

    /** 消息可见性 */
    public static final String VISIBLE_PUBLIC   = "public";
    public static final String VISIBLE_INTERNAL = "internal";

    /** run 状态 */
    public static final String RUN_RUNNING   = "running";
    public static final String RUN_WAITING   = "waiting_approval";
    public static final String RUN_SUCCESS   = "success";
    public static final String RUN_FAILED    = "failed";

    /** 审批状态 */
    public static final String APPROVAL_PENDING  = "pending";
    public static final String APPROVAL_APPROVED = "approved";
    public static final String APPROVAL_EXECUTING = "executing";
    public static final String APPROVAL_EXECUTED = "executed";
    public static final String APPROVAL_EXECUTION_FAILED = "execution_failed";
    public static final String APPROVAL_REJECTED = "rejected";
    public static final String APPROVAL_EXPIRED  = "expired";

    /** 工单状态码 → 中文名 */
    public static String ticketStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case TICKET_PENDING -> "待受理";
            case TICKET_PROCESSING -> "处理中";
            case TICKET_SUSPENDED -> "已挂起";
            case TICKET_CONFIRMING -> "待用户确认";
            case TICKET_RESOLVED -> "已解决";
            case TICKET_CLOSED -> "已关闭";
            default -> "未知(" + status + ")";
        };
    }

    /** 优先级码 → 中文名 */
    public static String priorityName(Integer priority) {
        if (priority == null) {
            return "未知";
        }
        return switch (priority) {
            case PRIORITY_URGENT -> "紧急";
            case PRIORITY_HIGH -> "高";
            case PRIORITY_NORMAL -> "中";
            case PRIORITY_LOW -> "低";
            default -> "未知(" + priority + ")";
        };
    }
}
