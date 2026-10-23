package com.atp.pojo.vo.kb;

import lombok.Data;

/**
 * 知识库引用。
 */
@Data
public class KbReferenceVO {

    // 引用编号，对应答案中的 [n]
    private Integer index;

    // 分块 ID
    private Long chunkId;

    // 文档 ID
    private Long documentId;

    // 文档标题
    private String docTitle;

    // 引用片段
    private String snippet;

    // 检索分数
    private Double score;
}
