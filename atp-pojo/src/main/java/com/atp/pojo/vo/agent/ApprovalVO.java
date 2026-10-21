package com.atp.pojo.vo.agent;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批单展示对象。
 */
@Data
public class ApprovalVO {

    // 审批单 ID
    private Long id;

    // 所属运行 ID
    private Long runId;

    // 所属会话 ID
    private Long sessionId;

    // 工具步骤序号
    private Integer stepSeq;

    // 工具编码
    private String toolCode;

    // 工具中文名
    private String toolName;

    // 工具参数 JSON
    private String arguments;

    // 审批原因
    private String reason;

    // 审批与执行状态
    private String status;

    // 审批人 ID
    private Long operatorId;

    // 审批时间
    private LocalDateTime operatedAt;

    // 审批意见
    private String comment;

    // 审批过期时间
    private LocalDateTime expireAt;

    // 创建时间
    private LocalDateTime createTime;
}
