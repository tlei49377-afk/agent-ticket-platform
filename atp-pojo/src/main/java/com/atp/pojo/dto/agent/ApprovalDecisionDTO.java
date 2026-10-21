package com.atp.pojo.dto.agent;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审批决定入参。
 */
@Data
public class ApprovalDecisionDTO {

    // 审批单 ID
    @NotNull(message = "审批单 ID 不能为空")
    private Long approvalId;

    // true 表示通过，false 表示驳回
    @NotNull(message = "审批决定不能为空")
    private Boolean approved;

    // 审批意见
    private String comment;
}
