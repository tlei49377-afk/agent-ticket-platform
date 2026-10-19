package com.atp.pojo.query;

import lombok.Data;

/**
 * 分页查询基类。所有列表查询 DTO 都继承它。
 */
@Data
public class PageQuery {

    //页码，从 1 开始
    private Integer page = 1;

    //每页条数
    private Integer pageSize = 10;

    /**
     * 取页码：没传或者小于 1 都按第 1 页处理
     * @return
     */
    public Integer getPage() {
        return (page == null || page < 1) ? 1 : page;
    }

    /**
     * 取每页条数：没传给 10，最大 100，避免前端传个超大值把库拖垮
     * @return
     */
    public Integer getPageSize() {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }
}
