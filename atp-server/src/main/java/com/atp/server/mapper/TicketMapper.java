package com.atp.server.mapper;

import com.atp.pojo.entity.ticket.Ticket;
import com.atp.pojo.query.TicketPageQueryDTO;
import com.atp.pojo.vo.ticket.TicketDetailVO;
import com.atp.pojo.vo.ticket.TicketVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单表访问。SQL 全部写在 resources/mapper/TicketMapper.xml 里。
 */
@Mapper
public interface TicketMapper {

    /** 新增，自增主键回填到 ticket.id */
    int insert(Ticket ticket);

    /**
     * 按 ID 查，逻辑删除的不算
     * @param id
     * @return
     */
    Ticket getById(@Param("id") Long id);

    /**
     * 按工单号查
     * @param ticketNo
     * @return
     */
    Ticket getByNo(@Param("ticketNo") String ticketNo);

    /** 列表：一条 SQL 连表带出分类名、提交人姓名、处理人姓名
     * @param query
     * @return
     */
    List<TicketVO> pageQuery(TicketPageQueryDTO query);

    /** 详情：比列表多带问题描述、首次响应时间等字段
     * @param id
     * @return
     */
    TicketDetailVO getVoById(@Param("id") Long id);

    /**
     * 按主键更新非空字段
     * @param ticket
     * @return
     */
    int update(Ticket ticket);

    /** 状态流转专用：只改状态、处理人、关闭时间，别的字段一律不动
     * @param id
     * @param status
     * @param assigneeId
     * @param closeAt
     * @return
     */
    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("assigneeId") Long assigneeId,
                     @Param("closeAt") LocalDateTime closeAt);

    /**
     * 写解决时间
     * @param id
     * @param resolvedAt
     * @return
     */
    int updateResolvedAt(@Param("id") Long id, @Param("resolvedAt") LocalDateTime resolvedAt);

    /** 只写第一个响应时间，已经写过就不再覆盖
     * @param id
     * @param firstRespondAt
     * @return
     */
    int updateFirstRespondAt(@Param("id") Long id,
                             @Param("firstRespondAt") LocalDateTime firstRespondAt);

    /**
     * 只换处理人
     * @param id
     * @param assigneeId
     * @return
     */
    int updateAssignee(@Param("id") Long id, @Param("assigneeId") Long assigneeId);

    /**
     * 只改优先级
     * @param id
     * @param priority
     * @return
     */
    int updatePriority(@Param("id") Long id, @Param("priority") Integer priority);

    /**
     * 重开次数 +1
     * @param id
     * @return
     */
    int increaseReopenCount(@Param("id") Long id);

    /** 逻辑删除
     * @param id
     * @return
     */
    int deleteById(@Param("id") Long id);

    /** 当日序号：数一下今天已经建了多少张单
     * @param prefix
     * @return
     */
    int countByNoPrefix(@Param("prefix") String prefix);

    /**
     * 按状态统计工单数
     * @param status
     * @return
     */
    int countByStatus(@Param("status") Integer status);

    /** 「待用户确认」超过 before 还没人确认的，2.7 自动流转用
     * @param before
     * @return
     */
    List<Ticket> listConfirmingBefore(@Param("before") LocalDateTime before);

    /** 「已解决」超过 before 还没关闭的，2.7 自动流转用
     * @param before
     * @return
     */
    List<Ticket> listResolvedBefore(@Param("before") LocalDateTime before);
}
