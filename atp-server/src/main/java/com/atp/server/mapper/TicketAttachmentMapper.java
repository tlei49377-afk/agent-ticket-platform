package com.atp.server.mapper;

import com.atp.pojo.entity.ticket.TicketAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工单附件访问。
 */
@Mapper
public interface TicketAttachmentMapper {

    /**
     * 新增附件元数据
     * @param attachment
     * @return
     */
    int insert(TicketAttachment attachment);

    /**
     * 查这张单的附件
     * @param ticketId
     * @return
     */
    List<TicketAttachment> listByTicket(@Param("ticketId") Long ticketId);
}
