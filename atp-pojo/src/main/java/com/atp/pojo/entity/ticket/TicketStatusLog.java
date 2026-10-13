package com.atp.pojo.entity.ticket;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单状态流转日志。每次流转写一行，是回放流程的唯一依据。
 */
@Data
public class TicketStatusLog {

    private Long id; // 主键
    private Long ticketId; // 工单 ID
    private Integer fromStatus; // 原状态，建单那条为 null
    private Integer toStatus; // 新状态
    private Long operatorId; // 操作人，系统自动流转时为空
    private String reason; // 原因
    private LocalDateTime createTime; // 创建时间
}
