package com.atp.server.mapper;

import com.atp.pojo.entity.ticket.SlaEscalationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * SLA 升级日志访问。
 */
@Mapper
public interface SlaEscalationLogMapper {

    /**
     * 记一条升级日志
     * @param log
     * @return
     */
    int insert(SlaEscalationLog log);

    /**
     * 查这张单的升级记录
     * @param ticketId
     * @return
     */
    List<SlaEscalationLog> listByTicket(@Param("ticketId") Long ticketId);
}
