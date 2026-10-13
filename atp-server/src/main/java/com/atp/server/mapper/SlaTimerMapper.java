package com.atp.server.mapper;

import com.atp.pojo.entity.ticket.SlaTimer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SLA 计时器访问。
 */
@Mapper
public interface SlaTimerMapper {

    /**
     * 新增一条计时器记录
     * @param timer
     * @return
     */
    int insert(SlaTimer timer);

    /**
     * 建单时一次插两条：响应型 + 解决型
     * @param list
     * @return
     */
    int insertBatch(@Param("list") List<SlaTimer> list);

    /**
     * 查这张单的计时器
     * @param ticketId
     * @return
     */
    List<SlaTimer> listByTicket(@Param("ticketId") Long ticketId);

    /**
     * 还没到期的，巡检时刷新状态用
     * */
    List<SlaTimer> listNotBreached();

    /**
     * 已超期且没升级过的，升级用
     * */
    List<SlaTimer> listBreachedNotEscalated();

    /**
     * 刷计时器状态，超期时同时记下超期时间
     * @param id
     * @param state
     * @param breachedAt
     * @return
     */
    int updateState(@Param("id") Long id,
                    @Param("state") String state,
                    @Param("breachedAt") LocalDateTime breachedAt);

    /** 抢锁式幂等标记：affectedRows == 1 才继续做升级动作 */
    int markEscalated(@Param("id") Long id);
}
