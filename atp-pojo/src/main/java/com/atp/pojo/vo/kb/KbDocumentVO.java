package com.atp.pojo.vo.kb;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档列表行。
 */
@Data
public class KbDocumentVO {

    // 文档 ID
    private Long id;

    // 文档标题
    private String title;

    // 文档类型
    private String docType;

    // 入库状态
    private String status;

    // 分块数量
    private Integer chunkCount;

    // 文档版本
    private Integer version;

    // 失败原因
    private String errorMsg;

    // 创建时间
    private LocalDateTime createTime;
}