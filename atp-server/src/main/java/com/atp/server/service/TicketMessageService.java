package com.atp.server.service;

import com.atp.pojo.dto.ticket.TicketReplyDTO;
import com.atp.pojo.vo.ticket.TicketMessageVO;

import java.util.List;

/**
 * 工单消息。公开回复和内部备注都走这里，可见性由服务端推导。
 */
public interface TicketMessageService {

    /** 内部人员看全部，其他人只看 public */
    List<TicketMessageVO> list(Long ticketId, Long currentUserId);

    /**
     * 回复。可见性由服务端按角色推导
     * @param ticketId
     * @param dto
     * @param senderId
     * @return
     */
    Long reply(Long ticketId, TicketReplyDTO dto, Long senderId);
}
