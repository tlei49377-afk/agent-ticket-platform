package com.atp.server.service.impl;

import com.atp.common.constant.MessageConstant;
import com.atp.common.constant.StatusConstant;
import com.atp.common.exception.BaseException;
import com.atp.pojo.dto.ticket.CategoryDTO;
import com.atp.pojo.entity.ticket.TicketCategory;
import com.atp.pojo.vo.ticket.CategoryTreeVO;
import com.atp.server.mapper.TicketCategoryMapper;
import com.atp.server.service.TicketCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工单分类实现。树在内存里拼，不做递归查库。
 */
@Service
public class TicketCategoryServiceImpl implements TicketCategoryService {

    @Autowired
    private TicketCategoryMapper categoryMapper;

    /**
     * 全部启用中的分类
     * @return
     */
    @Override
    public List<TicketCategory> listAll() {
        return categoryMapper.listEnabled();
    }

    /**
     * 在内存里把扁平列表拼成树
     * @return
     */
    @Override
    public List<CategoryTreeVO> tree() {
        List<TicketCategory> all = categoryMapper.listEnabled();
        Map<Long, CategoryTreeVO> nodeById = new LinkedHashMap<>();
        for (TicketCategory category : all) {
            nodeById.put(category.getId(), toNode(category));
        }

        List<CategoryTreeVO> roots = new ArrayList<>();
        for (TicketCategory category : all) {
            CategoryTreeVO node = nodeById.get(category.getId());
            CategoryTreeVO parent = nodeById.get(category.getParentId());
            if (parent == null) {
                // parent_id = 0，或者父分类被禁用了 —— 两种情况都按根节点处理，避免整棵子树消失
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    /**
     * 按 ID 查分类
     * @param id
     * @return
     */
    @Override
    public TicketCategory getById(Long id) {
        return id == null ? null : categoryMapper.getById(id);
    }

    /**
     * 新增分类，ancestors 按父分类路径拼
     * @param dto
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(CategoryDTO dto) {
        TicketCategory category = new TicketCategory();
        category.setParentId(dto.getParentId());
        category.setName(dto.getName());
        category.setSort(dto.getSort() == null ? 0 : dto.getSort());
        category.setDefaultPriority(dto.getDefaultPriority() == null
                ? StatusConstant.PRIORITY_NORMAL : dto.getDefaultPriority());

        // ancestors 存完整路径，以后要查子树可以一条 SQL 搞定
        if (dto.getParentId() == null || dto.getParentId() == 0L) {
            category.setParentId(0L);
            category.setAncestors("0");
        } else {
            TicketCategory parent = categoryMapper.getById(dto.getParentId());
            if (parent == null) {
                throw new BaseException(MessageConstant.TICKET_CATEGORY_NOT_FOUND);
            }
            category.setAncestors(parent.getAncestors() + "," + parent.getId());
        }

        categoryMapper.insert(category);
        return category.getId();
    }

    /**
     * 删除分类，下面还有未关闭的工单就不许删
     * @param id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (categoryMapper.getById(id) == null) {
            throw new BaseException(MessageConstant.TICKET_CATEGORY_NOT_FOUND);
        }
        if (categoryMapper.countOpenTickets(id) > 0) {
            throw new BaseException(MessageConstant.TICKET_CATEGORY_HAS_TICKET);
        }
        categoryMapper.deleteById(id);
    }

    /**
     * 实体转树节点
     * @param category
     * @return
     */
    private CategoryTreeVO toNode(TicketCategory category) {
        CategoryTreeVO node = new CategoryTreeVO();
        node.setId(category.getId());
        node.setParentId(category.getParentId());
        node.setName(category.getName());
        node.setDefaultPriority(category.getDefaultPriority());
        return node;
    }
}
