package com.atp.server.service.impl;

import com.atp.common.constant.StatusConstant;
import com.atp.pojo.entity.ticket.SlaEscalationLog;
import com.atp.pojo.entity.ticket.SlaTimer;
import com.atp.pojo.entity.ticket.Ticket;
import com.atp.server.mapper.SlaEscalationLogMapper;
import com.atp.server.mapper.SlaTimerMapper;
import com.atp.server.mapper.TicketMapper;
import com.atp.server.service.SlaEscalationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SLA 超期升级实现。先抢幂等标记，再动业务数据。
 */
@Service
@Slf4j
public class SlaEscalationServiceImpl implements SlaEscalationService {

    @Autowired
    private SlaTimerMapper slaTimerMapper;
    @Autowired
    private TicketMapper ticketMapper;
    @Autowired
    private SlaEscalationLogMapper escalationLogMapper;

    /**
     * 升级：先抢幂等标记，抢到才提优先级并写升级日志
     * @param timer
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void escalate(SlaTimer timer) {
        // 用带条件的 UPDATE 抢锁：抢不到说明别的线程或别的实例已经在处理了
        if (slaTimerMapper.markEscalated(timer.getId()) != 1) {
            return;
        }

        Ticket ticket = ticketMapper.getById(timer.getTicketId());
        if (ticket == null || ticket.getStatus() == null
                || ticket.getStatus() >= StatusConstant.TICKET_RESOLVED) {
            return;
        }

        Integer from = ticket.getPriority();
        // 一次只提一级，最高提到「紧急」，避免一次跳到顶
        Integer to = (from == null || from <= StatusConstant.PRIORITY_URGENT) ? from : from - 1;
        boolean raised = to != null && !to.equals(from);
        if (raised) {
            ticketMapper.updatePriority(ticket.getId(), to);
        }

        SlaEscalationLog row = new SlaEscalationLog();
        row.setTicketId(ticket.getId());
        row.setTimerId(timer.getId());
        row.setRuleName(StatusConstant.TIMER_RESPOND.equals(timer.getTimerType())
                ? "响应超期升级" : "解决超期升级");
        row.setAction(raised
                ? "优先级 " + StatusConstant.priorityName(from) + " 提到 " + StatusConstant.priorityName(to)
                : "已是最高优先级，仅通知");
        row.setFromPriority(from);
        row.setToPriority(to);
        row.setNotifyTo("supervisor");
        row.setSuccess(1);
        escalationLogMapper.insert(row);

        log.warn("工单 {} SLA 超期，升级动作：{}", ticket.getTicketNo(), row.getAction());
    }
}
