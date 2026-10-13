package com.atp.pojo.vo.ticket;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单详情。列表行的字段 + 详情专属字段 + 下一跳状态 + 往来消息。
 */
@Data
@EqualsAndHashCode(callSuper = true) // 继承父类的字段
public class TicketDetailVO extends TicketVO {

    //问题描述
    private String content;

    //首次响应时间，客服的第一条公开回复发生时写入
    private LocalDateTime firstRespondAt;

    //解决时间
    private LocalDateTime resolvedAt;

    //关闭时间
    private LocalDateTime closedAt;

    //重开次数
    private Integer reopenCount;

    //当前状态允许流转到哪些状态，前端据此渲染按钮
    private List<Integer> allowedNextStatus;

    //往来消息，提交人只会拿到 public 的
    private List<TicketMessageVO> messages;
}
