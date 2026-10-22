package com.atp.pojo.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库文档分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KbDocumentPageQueryDTO extends PageQuery {

    // 标题关键词。
    private String keyword;

    // 文档类型：file/faq。
    private String docType;

    // 文档状态：pending/processing/ready/failed。
    private String status;
}