package com.atp.pojo.entity.ticket;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * SLA 计时器。一张工单两条：响应型、解决型。
 */
@Data
public class SlaTimer {

    private Long id; // 主键
    private Long ticketId; // 工单 ID
    private String timerType; // 类型：respond 响应型 / resolve 解决型
    private LocalDateTime deadline; // 截止时间
    private String state; // 状态：normal 正常 / warn 临期 / breached 已超期
    private LocalDateTime breachedAt; // 实际超期时间
    private Integer escalated; // 是否已执行升级，幂等标记
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
}
