package com.atp.server.service;

import com.atp.common.result.PageResult;
import com.atp.pojo.dto.ticket.TicketCreateDTO;
import com.atp.pojo.query.TicketPageQueryDTO;
import com.atp.pojo.vo.ticket.TicketDetailVO;
import com.atp.pojo.vo.ticket.TicketStatusLogVO;
import com.atp.pojo.vo.ticket.TicketVO;

import java.util.List;

/**
 * 工单读写。currentUserId 一律显式往下传，不在 Service 里取 ThreadLocal。
 */
public interface TicketService {

    /**
     * 分页查询。currentUserId 决定能看到哪些工单
     * @param query
     * @param currentUserId
     * @return
     */
    PageResult<TicketVO> pageQuery(TicketPageQueryDTO query, Long currentUserId);

    /**
     * 工单详情
     * @param ticketId
     * @param currentUserId
     * @return
     */
    TicketDetailVO detail(Long ticketId, Long currentUserId);

    /**
     * 状态变更历史
     * @param ticketId
     * @param currentUserId
     * @return
     */
    List<TicketStatusLogVO> history(Long ticketId, Long currentUserId);

    /**
     * 建单，同时算好 SLA 截止时间并开两个计时器
     * @param dto
     * @param currentUserId
     * @return
     */
    Long create(TicketCreateDTO dto, Long currentUserId);

    /**
     * 指派 / 转派
     * @param ticketId
     * @param assigneeId
     * @param reason
     * @param operatorId
     */
    void assign(Long ticketId, Long assigneeId, String reason, Long operatorId);

    /**
     * 调整优先级
     * @param ticketId
     * @param targetPriority
     * @param reason
     * @param operatorId
     */
    void changePriority(Long ticketId, Integer targetPriority, String reason, Long operatorId);

    /**
     * 逻辑删除
     * @param ticketId
     * @param operatorId
     */
    void delete(Long ticketId, Long operatorId);
}
