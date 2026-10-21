package com.atp.pojo.entity.agent;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 写操作审批单，对应 agent_approval 表。
 *
 * 审批单保存的是“准备执行什么”，不是执行结果。真正的执行发生在
 * 审批通过后的 resume 流程中。
 */
@Data
public class AgentApproval {

    private Long id; // 主键
    private Long runId; // 所属运行 ID
    private Long sessionId; // 所属会话 ID
    private Integer stepSeq; // 发起审批时的工具步骤序号
    private String toolCode; // 待执行工具编码
    private String arguments; // 工具参数 JSON
    private String reason; // 审批原因

    // pending/approved/executing/executed/execution_failed/rejected/expired
    private String status;

    private Long operatorId; // 审批人 ID
    private LocalDateTime operatedAt; // 审批时间
    private String comment; // 审批意见
    private LocalDateTime expireAt; // 审批过期时间
    private LocalDateTime createTime; // 创建时间
}
