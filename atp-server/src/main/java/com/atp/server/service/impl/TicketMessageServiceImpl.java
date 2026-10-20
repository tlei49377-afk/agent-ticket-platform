package com.atp.server.service.impl;

import com.atp.common.constant.MessageConstant;
import com.atp.common.constant.StatusConstant;
import com.atp.common.exception.BaseException;
import com.atp.common.exception.PermissionDeniedException;
import com.atp.pojo.dto.ticket.TicketReplyDTO;
import com.atp.pojo.entity.ticket.Ticket;
import com.atp.pojo.entity.ticket.TicketMessage;
import com.atp.pojo.vo.ticket.TicketMessageVO;
import com.atp.server.mapper.TicketMapper;
import com.atp.server.mapper.TicketMessageMapper;
import com.atp.server.service.TicketMessageService;
import com.atp.server.service.UserAccessService;
import com.atp.server.statemachine.TicketStateMachine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单消息实现。可见性由服务端根据角色推导，前端传什么都不作数。
 */
@Service
@RequiredArgsConstructor
public class TicketMessageServiceImpl implements TicketMessageService {

    private final TicketMessageMapper messageMapper;
    private final TicketMapper ticketMapper;
    private final UserAccessService userAccessService;

    /**
     * 内部人员看全部消息，其他人只看 public
     * @param ticketId
     * @param currentUserId
     * @return
     */
    @Override
    public List<TicketMessageVO> list(Long ticketId, Long currentUserId) {
        // 内部人员 visible 传 null（不过滤），其他人固定只能看 public
        boolean staff = userAccessService.isInternalStaff(currentUserId);
        return messageMapper.listByTicket(ticketId, staff ? null : StatusConstant.VISIBLE_PUBLIC);
    }

    /**
     * 回复：定可见性、写消息、必要时记首次响应时间
     * @param ticketId
     * @param dto
     * @param senderId
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long reply(Long ticketId, TicketReplyDTO dto, Long senderId) {
        Ticket ticket = ticketMapper.getById(ticketId);
        if (ticket == null) {
            throw new BaseException(MessageConstant.TICKET_NOT_FOUND);
        }
        if (TicketStateMachine.isFinal(ticket.getStatus())) {
            throw new BaseException(MessageConstant.TICKET_CLOSED_CANNOT_REPLY);
        }

        boolean staff = userAccessService.isInternalStaff(senderId);
        // 提交人只能在自己的单子上说话
        if (!staff && !senderId.equals(ticket.getRequesterId())) {
            throw new PermissionDeniedException(MessageConstant.TICKET_NOT_OWNED);
        }
        if (dto.getVisible() != null
                && !StatusConstant.VISIBLE_PUBLIC.equals(dto.getVisible())
                && !StatusConstant.VISIBLE_INTERNAL.equals(dto.getVisible())) {
            throw new BaseException(MessageConstant.VISIBLE_ILLEGAL);
        }
        // 提交人就算传 internal 也会被降级成 public，内部备注不能流到客户那边
        String visible = StatusConstant.VISIBLE_INTERNAL.equals(dto.getVisible()) && staff
                ? StatusConstant.VISIBLE_INTERNAL
                : StatusConstant.VISIBLE_PUBLIC;

        TicketMessage message = new TicketMessage();
        message.setTicketId(ticketId);
        message.setRole(staff ? "agent" : "requester");
        message.setSenderId(senderId);
        message.setContent(dto.getContent());
        message.setVisible(visible);
        messageMapper.insert(message);

        // 客服的第一条公开回复 = 首次响应时间，响应型 SLA 到点。内部备注不算，客户没收到
        if (staff && StatusConstant.VISIBLE_PUBLIC.equals(visible) && ticket.getFirstRespondAt() == null) {
            ticketMapper.updateFirstRespondAt(ticketId, LocalDateTime.now());
        }
        return message.getId();
    }
}
