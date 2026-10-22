package com.atp.pojo.entity.kb;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档，对应 kb_document 表。
 *
 * 状态流转为 pending -> processing -> ready/failed。
 */
@Data
public class KbDocument {


    private Long id; // 主键

    private String title; // 文档标题

    private String docType; // 文档类型，当前为 file

    private String sourceUrl; // 原始来源 URL

    private String filePath; // 本地文件路径

    private String status; // 入库状态

    private Integer chunkCount; // 分块数量

    private Integer version; // 文档版本

    private String errorMsg; // 处理失败原因

    private Long uploadedBy; // 上传人 ID

    private LocalDateTime createTime; // 创建时间

    private LocalDateTime updateTime; // 更新时间

    private Integer deleted; // 逻辑删除标记
}