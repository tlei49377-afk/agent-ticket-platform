package com.atp.pojo.query;

import lombok.Data;

/**
 * 分页查询基类。所有列表查询 DTO 都继承它。
 */
@Data
public class PageQuery {
    private Integer page = 1;
    private Integer pageSize = 10;

    public Integer getPage(){
        return (page == null || page < 1) ? 1 : page;
    }

    public Integer getPageSize(){
        if(pageSize == null || pageSize < 1){
            return 10;
        }
        return Math.min(pageSize, 100);
    }
}
