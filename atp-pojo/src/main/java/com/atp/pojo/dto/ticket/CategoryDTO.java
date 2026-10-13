package com.atp.pojo.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增工单分类入参。对应前端 POST /categories。
 */
@Data
public class CategoryDTO {

    //父分类 ID，根分类传 0
    @NotNull(message = "父分类不能为空，根分类传 0")
    private Long parentId;

    //分类名称
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 64, message = "分类名称最长 64 个字符")
    private String name;

    //排序，越小越靠前
    private Integer sort;

    //该分类下新工单的默认优先级 1 紧急 2 高 3 中 4 低
    private Integer defaultPriority;
}
