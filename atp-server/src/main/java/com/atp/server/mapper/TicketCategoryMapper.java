package com.atp.server.mapper;

import com.atp.pojo.entity.ticket.TicketCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工单分类访问。
 */
@Mapper
public interface TicketCategoryMapper {

    /** 启用中的分类，全查出来在内存里拼树 */
    List<TicketCategory> listEnabled();

    /**
     * 按 ID 查，逻辑删除的不算
     * @param id
     * @return
     */
    TicketCategory getById(@Param("id") Long id);

    /**
     * 新增分类，主键回填
     * @param category
     * @return
     */
    int insert(TicketCategory category);

    /** 逻辑删除
     * @param id
     * @return
     */
    int deleteById(@Param("id") Long id);

    /** 该分类下还有多少张没关闭的工单，删除前先问它
     * @param categoryId
     * @return
     */
    int countOpenTickets(@Param("categoryId") Long categoryId);
}
