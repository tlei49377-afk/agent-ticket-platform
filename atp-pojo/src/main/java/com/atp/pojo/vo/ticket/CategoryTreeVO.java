package com.atp.pojo.vo.ticket;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 分类树节点。前端新建工单时用它渲染级联/下拉。
 */
@Data
public class CategoryTreeVO {

    //分类 ID
    private Long id;

    //父分类 ID，0 为根
    private Long parentId;

    //分类名称
    private String name;

    //该分类下新工单的默认优先级
    private Integer defaultPriority;

    //子分类
    private List<CategoryTreeVO> children = new ArrayList<>();
}
