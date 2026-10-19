package com.atp.pojo.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 工单分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TicketPageQueryDTO extends PageQuery {

    //状态：1 待受理 2 处理中 3 已挂起 4 待用户确认 5 已解决 6 已关闭
    private Integer status;

    //优先级：1 紧急 2 高 3 中 4 低
    private Integer priority;

    //分类 ID
    private Long categoryId;

    //来源渠道：web/mail/api/phone/ai
    private String channel;

    //提交人 ID；提交人角色只查自己的工单，Service 层会强制覆盖这个值
    private Long requesterId;

    //当前处理人 ID
    private Long assigneeId;

    //关键词，模糊匹配工单号/标题/描述
    private String keyword;

    //创建时间起（含）
    private LocalDateTime createTimeStart;

    //创建时间止（含）
    private LocalDateTime createTimeEnd;
}
