package com.atp.pojo.vo.ticket;

import lombok.Data;

/**
 * 工单统计出参。
 */
@Data
public class TicketStatsVO {

    /**
     * 工单总量。
     */
    private Long total;

    /**
     * 已解决或已关闭的工单数。
     */
    private Long resolvedCount;

    /**
     * 仍未解决但已超过解决期限的工单数。
     */
    private Long breachedCount;

    /**
     * 平均首次响应时长，单位分钟。
     */
    private Double avgRespondMinutes;
}