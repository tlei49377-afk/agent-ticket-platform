package com.atp.pojo.entity.agent;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工具元数据，对应 agent_tool 表。
 */
@Data
public class AgentToolMeta {

    private Long id; // 主键
    private String code; // 工具编码，和 AgentTool.code() 必须一致
    private String name; // 中文名
    private String description; // 给模型看的描述
    private String paramSchema; // 参数 JSON Schema
    private Integer readOnly; // 是否只读：1 是 0 否
    private Integer idempotent; // 是否幂等
    private Integer timeoutMs; // 超时毫秒
    private Integer maxRetry; // 最大重试次数
    private Integer retryBackoffMs; // 重试退避基数
    private String requiredPermission; // 所需权限码
    private Integer needApproval; // 是否需要人工审批
    private String externalSystem; // 外部系统名，外部工具才有
    private Integer enabled; // 是否启用
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
}