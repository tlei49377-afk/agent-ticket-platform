package com.atp.server.service;

import com.atp.pojo.dto.ticket.CategoryDTO;
import com.atp.pojo.entity.ticket.TicketCategory;
import com.atp.pojo.vo.ticket.CategoryTreeVO;

import java.util.List;

/**
 * 工单分类。
 */
public interface TicketCategoryService {

    /** 扁平列表，建单页的二级下拉用 */
    List<TicketCategory> listAll();

    /** 树形，前端级联选择器用 */
    List<CategoryTreeVO> tree();

    /**
     * 按 ID 查分类
     * @param id
     * @return
     */
    TicketCategory getById(Long id);

    /**
     * 新增分类
     * @param dto
     * @return
     */
    Long save(CategoryDTO dto);

    /**
     * 删除分类，下面还挂着未关闭的工单就拒绝
     * @param id
     */
    void delete(Long id);
}
