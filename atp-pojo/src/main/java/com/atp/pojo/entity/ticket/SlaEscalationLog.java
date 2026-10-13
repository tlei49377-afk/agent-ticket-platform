package com.atp.pojo.entity.ticket;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * SLA 升级日志。每次超期升级写一行，用于复盘谁在什么时候被升级通知到。
 */
@Data
public class SlaEscalationLog {

    private Long id; // 主键
    private Long ticketId; // 工单 ID
    private Long timerId; // 计时器 ID
    private String ruleName; // 触发规则名
    private String action; // 升级动作描述
    private Integer fromPriority; // 原优先级
    private Integer toPriority; // 新优先级
    private String notifyTo; // 通知对象
    private Integer success; // 是否成功：1 是 0 否
    private String errorMsg; // 失败原因
    private LocalDateTime createTime; // 创建时间
}
