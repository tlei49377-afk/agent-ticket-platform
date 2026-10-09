package com.atp.pojo.entity.ticket;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Ticket {
    private Long id; // 主键
    private String ticketNo; // 工单号，如 TK202610010001
    private String title; // 标题
    private String content; // 问题描述
    private String channel; // 来源渠道：web/mail/api/phone/ai
    private Long categoryId; // 分类 ID
    private Integer priority; // 优先级：1 紧急 2 高 3 中 4 低
    private Integer status; // 状态：1 待受理 2 处理中 3 已挂起 4 待用户确认 5 已解决 6 已关闭
    private Long requesterId; // 提交人 ID
    private Long assigneeId; // 当前处理人 ID
    private Long slaPolicyId; // SLA 策略 ID
    private LocalDateTime respondDeadline; // 响应截止时间
    private LocalDateTime resolveDeadline; // 解决截止时间
    private LocalDateTime firstRespondAt; // 首次响应时间
    private LocalDateTime resolvedAt; // 解决时间
    private LocalDateTime closedAt; // 关闭时间
    private Integer reopenCount; // 重开次数
    private Integer satisfaction; // 满意度 1~5
    private String satisfactionNote; // 满意度评语
    private Integer aiHandled; // 是否 AI 参与处理
    private Long aiRunId; // AI 处理对应的 run
    private Long createBy; // 创建人
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
    private Integer deleted; // 逻辑删除：0 未删 1 已删
}
