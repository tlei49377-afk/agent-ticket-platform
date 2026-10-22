package com.atp.pojo.entity.kb;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 检索日志，对应 kb_retrieval_log 表。
 *
 * 它是评测、调参和引用溯源的原始数据。
 */
@Data
public class KbRetrievalLog {

    // 主键
    private Long id;

    // 查询文本
    private String queryText;

    // 所属 Agent run ID
    private Long runId;

    // 检索模式：vector/keyword/hybrid
    private String mode;

    // 返回条数
    private Integer topK;

    // 命中的 chunk/faq ID JSON 数组
    private String resultIds;

    // 对应分数 JSON 数组
    private String resultScores;

    // 最高分
    private BigDecimal top1Score;

    // 检索耗时毫秒
    private Integer costMs;

    // 创建时间
    private LocalDateTime createTime;
}