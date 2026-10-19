package com.atp.pojo.entity.agent;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Agent 会话，对应 agent_session 表。
 */
@Data
public class AgentSession {

    private Long id; // 主键
    private Long agentId; // Agent ID
    private Long userId; // 归属用户
    private String title; // 会话标题，取首句
    private String memoryId; // 记忆 ID，格式 {userId}:{sessionId}
    private Long ticketId; // 关联工单，可空
    private LocalDateTime lastActiveAt; // 最后活跃时间
    private Integer status; // 1 活跃 0 归档
    private LocalDateTime createTime; // 创建时间
    private Integer deleted; // 逻辑删除
}