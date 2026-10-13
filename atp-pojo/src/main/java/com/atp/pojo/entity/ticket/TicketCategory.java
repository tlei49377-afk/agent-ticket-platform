package com.atp.pojo.entity.ticket;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单分类。两级：parent_id = 0 是根。
 */
@Data
public class TicketCategory {

    private Long id; // 主键
    private Long parentId; // 父 ID，0 为根
    private String name; // 分类名称
    private String ancestors; // 祖先路径，如 0,1,3
    private Integer sort; // 排序
    private Integer defaultPriority; // 默认优先级：1 紧急 2 高 3 中 4 低
    private Integer status; // 状态：1 启用 0 禁用
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
    private Integer deleted; // 逻辑删除：0 未删 1 已删
}
