package com.atp.server.controller;

import com.atp.common.annotation.RequirePermission;
import com.atp.common.result.Result;
import com.atp.pojo.dto.ticket.CategoryDTO;
import com.atp.pojo.entity.ticket.TicketCategory;
import com.atp.pojo.vo.ticket.CategoryTreeVO;
import com.atp.server.service.TicketCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工单分类接口。查询不加权限码：提交人建单时也要选分类。
 */
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final TicketCategoryService categoryService;

    /**
     * 分类树，前端级联选择器用
     * @return
     */
    @GetMapping("/tree")
    public Result<List<CategoryTreeVO>> tree() {
        return Result.success(categoryService.tree());
    }

    /**
     * 分类平铺列表
     * @return
     */
    @GetMapping("/flat")
    public Result<List<TicketCategory>> flat() {
        return Result.success(categoryService.listAll());
    }

    /**
     * 新增分类
     * @param dto
     * @return
     */
    @PostMapping
    @RequirePermission("ticket:update")
    public Result<Long> save(@Valid @RequestBody CategoryDTO dto) {
        return Result.success(categoryService.save(dto));
    }

    /**
     * 删除分类
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    @RequirePermission("ticket:update")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success();
    }
}
