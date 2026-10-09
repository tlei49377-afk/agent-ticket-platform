package com.atp.common.constant;

/**
 * 业务状态码常量。
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
    public static final String APPROVAL_REJECTED = "rejected";
    public static final String APPROVAL_EXPIRED  = "expired";

    private StatusConstant() {
    }
}
