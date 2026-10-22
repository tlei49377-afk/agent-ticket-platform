package com.atp.pojo.entity.kb;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * FAQ 词条，对应 kb_faq 表。
 *
 * 一条问答作为一个原子语义单元，不切分。
 */
@Data
public class KbFaq {

    // 主键
    private Long id;

    // 标准问题
    private String question;

    // 相似问法 JSON 数组
    private String similarQuestions;

    // 标准答案
    private String answer;

    // 业务分类
    private String category;

    // Redis 向量主键
    private String vectorId;

    // 内容哈希，用于去重
    private String contentHash;

    // 命中次数
    private Integer hitCount;

    // 状态：1 启用，0 停用
    private Integer status;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;

    // 逻辑删除标记
    private Integer deleted;
}