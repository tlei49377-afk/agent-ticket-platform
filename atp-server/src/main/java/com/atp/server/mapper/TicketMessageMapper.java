package com.atp.server.mapper;

import com.atp.pojo.entity.ticket.TicketMessage;
import com.atp.pojo.vo.ticket.TicketMessageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工单消息访问。visible 传 null 表示不过滤，内部人员看全部。
 */
@Mapper
public interface TicketMessageMapper {

    /**
     * 新增一条消息
     * @param message
     * @return
     */
    int insert(TicketMessage message);

    /**
     * 查这张单的消息，带出发送人姓名
     * @param ticketId
     * @param visible
     * @return
     */
    List<TicketMessageVO> listByTicket(@Param("ticketId") Long ticketId,
                                       @Param("visible") String visible);
}
