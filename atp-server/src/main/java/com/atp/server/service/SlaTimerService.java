package com.atp.server.service;

import com.atp.pojo.entity.ticket.SlaPolicy;
import com.atp.pojo.entity.ticket.SlaTimer;
import com.atp.pojo.entity.ticket.Ticket;

import java.util.List;

/**
 * SLA 计时器。
 */
public interface SlaTimerService {

    /** 建单时用：按策略生成响应型 + 解决型两条计时器 */
    void createTimers(Ticket ticket, SlaPolicy policy);

    /**
     * 这张单的两个计时器
     * @param ticketId
     * @return
     */
    List<SlaTimer> listByTicket(Long ticketId);

    /** 巡检第一步：把到期的判成已超期，把快到期的判成临期 */
    void refreshStates();

    /** 巡检第二步：捞出超期且没升级过的计时器 */
    List<SlaTimer> breachedNotEscalated();

    /**
     * 查询还没判超期的计时器，SLA 查询工具用它捞“即将超期”的数据。
     *
     * @return 未超期计时器列表
     */
    List<SlaTimer> listNotBreached();
}
