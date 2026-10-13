package com.atp.pojo.entity.ticket;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * SLA 策略。一个优先级一条，决定这张单多久必须响应、多久必须解决。
 */
@Data
public class SlaPolicy {

    private Long id; // 主键
    private String name; // 策略名称
    private Integer priority; // 适用优先级：1 紧急 2 高 3 中 4 低
    private Integer respondMinutes; // 响应时限（分钟）
    private Integer resolveMinutes; // 解决时限（分钟）
    private Integer warnPercent; // 临期阈值：剩余时间低于该百分比视为临期
    private String escalateRole; // 超期升级通知的角色编码
    private Integer enabled; // 是否启用：1 启用 0 停用
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
    private Integer deleted; // 逻辑删除：0 未删 1 已删
}
