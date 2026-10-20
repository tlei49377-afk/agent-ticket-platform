package com.atp.pojo.entity.sys;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志，对应 sys_audit_log 表。工具执行与关键操作都写这里。
 */
@Data
public class SysAuditLog {

    private Long id; // 主键
    private Long userId; // 操作人 ID
    private String username; // 操作人姓名（冗余，方便查）
    private String action; // 动作：TOOL_EXEC/APPROVE/LOGIN/...
    private String toolCode; // 工具编码（工具类动作才有）
    private String targetType; // 目标类型：TICKET/USER/...
    private Long targetId; // 目标 ID
    private String detail; // 详情（参数、结果摘要），JSON 字符串
    private Integer success; // 是否成功：1 是 0 否
    private String errorMsg; // 失败原因
    private String ip; // 来源 IP
    private String traceId; // 链路 ID
    private Integer costMs; // 耗时毫秒
    private LocalDateTime createTime; // 创建时间
}