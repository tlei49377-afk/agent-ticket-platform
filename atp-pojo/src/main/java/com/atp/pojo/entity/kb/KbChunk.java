package com.atp.pojo.entity.kb;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库分块，对应 kb_chunk 表。
 *
 * content_hash 有唯一索引，用于避免重复向量化。
 */
@Data
public class KbChunk {


    private Long id; // 主键


    private Long documentId; // 所属文档 ID

    private Integer seq; // 文档内分块序号

    private String content; // 分块正文

    private Integer tokenCount; // 估算 token 数

    private String vectorId; // Redis 向量库中的主键

    private String embeddingModel; // 生成向量使用的模型名

    private String contentHash; // 内容 SHA-256

    private Integer pageNo; // PDF 页码，可空

    private String category; // 业务分类，可空

    private LocalDateTime createTime; // 创建时间
}