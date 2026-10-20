package com.atp.server.service;

import com.atp.common.constant.MessageConstant;
import com.atp.common.constant.StatusConstant;
import com.atp.common.exception.BaseException;
import com.atp.pojo.entity.ticket.Ticket;
import com.atp.pojo.entity.ticket.TicketStatusLog;
import com.atp.server.mapper.TicketMapper;
import com.atp.server.mapper.TicketStatusLogMapper;
import com.atp.server.statemachine.TicketStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 工单状态流转的唯一入口。任何人要改状态都调它，别在别处直接 update status。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TicketStatusService {

    private final TicketMapper ticketMapper;
    private final TicketStatusLogMapper statusLogMapper;

    /**
     * 校验 → 更新 → 留痕，三步在同一个事务里。
     * @param ticketId
     * @param targetStatus
     * @param operatorId 操作人；定时任务自动流转时传 null
     * @param reason
     */
    @Transactional(rollbackFor = Exception.class)
    public void transit(Long ticketId, Integer targetStatus, Long operatorId, String reason) {
        Ticket ticket = ticketMapper.getById(ticketId);
        if (ticket == null) {
            throw new BaseException(MessageConstant.TICKET_NOT_FOUND);
        }

        Integer from = ticket.getStatus();
        TicketStateMachine.check(from, targetStatus);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime closedAt = targetStatus == StatusConstant.TICKET_CLOSED ? now : null;

        // 待受理 → 处理中 时如果还没人认领，谁操作就归谁，省一次指派
        Long assigneeId = null;
        if (from == StatusConstant.TICKET_PENDING
                && targetStatus == StatusConstant.TICKET_PROCESSING
                && ticket.getAssigneeId() == null) {
            assigneeId = operatorId;
        }

        ticketMapper.updateStatus(ticketId, targetStatus, assigneeId, closedAt);
        if (targetStatus == StatusConstant.TICKET_RESOLVED) {
            ticketMapper.updateResolvedAt(ticketId, now);
        }
        if (TicketStateMachine.isReopen(from, targetStatus)) {
            ticketMapper.increaseReopenCount(ticketId);
        }

        // 变量名不能叫 log，会把 @Slf4j 的 logger 盖掉
        TicketStatusLog logRow = new TicketStatusLog();
        logRow.setTicketId(ticketId);
        logRow.setFromStatus(from);
        logRow.setToStatus(targetStatus);
        logRow.setOperatorId(operatorId);
        logRow.setReason(reason);
        statusLogMapper.insert(logRow);

        log.info("工单 {} 状态流转：{} → {}，操作人 {}，原因 {}",
                ticket.getTicketNo(),
                StatusConstant.ticketStatusName(from),
                StatusConstant.ticketStatusName(targetStatus),
                operatorId, reason);
    }
}
