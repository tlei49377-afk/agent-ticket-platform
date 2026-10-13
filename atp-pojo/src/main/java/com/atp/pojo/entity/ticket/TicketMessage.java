package com.atp.pojo.entity.ticket;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单消息。一条公开回复，或一条只有内部人看得见的备注。
 */
@Data
public class TicketMessage {

    private Long id; // 主键
    private Long ticketId; // 工单 ID
    private String role; // 发送人角色：requester/agent/ai/system
    private Long senderId; // 发送人 ID，system 和 ai 可以为空
    private String content; // 内容
    private String visible; // 可见性：public 用户可见 / internal 内部备注
    private Long aiRunId; // AI 生成时对应的 run
    private LocalDateTime createTime; // 创建时间
    private Integer deleted; // 逻辑删除：0 未删 1 已删
}
