package com.atp.server.mapper;

import com.atp.pojo.entity.ticket.TicketStatusLog;
import com.atp.pojo.vo.ticket.TicketStatusLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工单状态流转日志访问。只增不改，是回放流程的唯一依据。
 */
@Mapper
public interface TicketStatusLogMapper {

    /**
     * 记一条流转日志，只增不改
     * @param log
     * @return
     */
    int insert(TicketStatusLog log);

    /**
     * 详情页的状态变更历史，带出操作人姓名
     * @param ticketId
     * @return
     */
    List<TicketStatusLogVO> listByTicket(@Param("ticketId") Long ticketId);
}
