package com.atp.server.service.impl;

import com.atp.common.constant.StatusConstant;
import com.atp.pojo.entity.ticket.SlaPolicy;
import com.atp.pojo.entity.ticket.SlaTimer;
import com.atp.pojo.entity.ticket.Ticket;
import com.atp.server.mapper.SlaPolicyMapper;
import com.atp.server.mapper.SlaTimerMapper;
import com.atp.server.mapper.TicketMapper;
import com.atp.server.service.SlaTimerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * SLA 计时器实现。巡检在这里刷新状态，超期判定只认「截止时间到了没」。
 */
@Service
public class SlaTimerServiceImpl implements SlaTimerService {

    @Autowired
    private SlaTimerMapper slaTimerMapper;
    @Autowired
    private TicketMapper ticketMapper;
    @Autowired
    private SlaPolicyMapper slaPolicyMapper;

    /**
     * 建单时开两个计时器：响应型 + 解决型
     * @param ticket
     * @param policy
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTimers(Ticket ticket, SlaPolicy policy) {
        LocalDateTime now = LocalDateTime.now();
        List<SlaTimer> timers = List.of(
                buildTimer(ticket.getId(), StatusConstant.TIMER_RESPOND,
                        now.plusMinutes(policy.getRespondMinutes())),
                buildTimer(ticket.getId(), StatusConstant.TIMER_RESOLVE,
                        now.plusMinutes(policy.getResolveMinutes()))
        );
        slaTimerMapper.insertBatch(timers);
    }

    /**
     * 这张单的计时器
     * @param ticketId
     * @return
     */
    @Override
    public List<SlaTimer> listByTicket(Long ticketId) {
        return slaTimerMapper.listByTicket(ticketId);
    }

    /**
     * 把已到期的刷成 breached、快到期的刷成 warn；已解决/已关闭的单子跳过
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshStates() {
        LocalDateTime now = LocalDateTime.now();
        for (SlaTimer timer : slaTimerMapper.listNotBreached()) {
            Ticket ticket = ticketMapper.getById(timer.getTicketId());
            // 单子已经解决/关闭，就不用再算它超没超期了
            if (ticket == null || ticket.getStatus() == null
                    || ticket.getStatus() >= StatusConstant.TICKET_RESOLVED) {
                continue;
            }

            if (!now.isBefore(timer.getDeadline())) {
                slaTimerMapper.updateState(timer.getId(), StatusConstant.SLA_BREACHED, now);
                continue;
            }

            SlaPolicy policy = ticket.getSlaPolicyId() == null
                    ? null : slaPolicyMapper.getById(ticket.getSlaPolicyId());
            if (policy == null) {
                continue;
            }
            long totalMinutes = StatusConstant.TIMER_RESPOND.equals(timer.getTimerType())
                    ? policy.getRespondMinutes() : policy.getResolveMinutes();
            long remainMinutes = Duration.between(now, timer.getDeadline()).toMinutes();
            long warnMinutes = totalMinutes * policy.getWarnPercent() / 100;

            String state = remainMinutes <= warnMinutes ? StatusConstant.SLA_WARN : StatusConstant.SLA_NORMAL;
            if (!state.equals(timer.getState())) {
                slaTimerMapper.updateState(timer.getId(), state, null);
            }
        }
    }

    /**
     * 超期且还没升级过的计时器
     * @return
     */
    @Override
    public List<SlaTimer> breachedNotEscalated() {
        return slaTimerMapper.listBreachedNotEscalated();
    }

    /**
     * 构造一条计时器记录
     * @param ticketId
     * @param timerType
     * @param deadline
     * @return
     */
    private SlaTimer buildTimer(Long ticketId, String timerType, LocalDateTime deadline) {
        SlaTimer timer = new SlaTimer();
        timer.setTicketId(ticketId);
        timer.setTimerType(timerType);
        timer.setDeadline(deadline);
        timer.setState(StatusConstant.SLA_NORMAL);
        timer.setEscalated(0);
        return timer;
    }
}
