package com.atp.pojo.vo.kb;

import com.atp.pojo.entity.kb.KbChunk;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库文档详情。
 *
 * <p>列表页只需要元数据，详情弹窗需要正文和分块，所以单独提供该 VO，
 * 避免把大字段放进分页列表响应。</p>
 */
@Data
public class KbDocumentDetailVO {

    // 文档 ID
    private Long id;

    // 文档标题
    private String title;

    // 文档类型：file/faq
    private String docType;

    // 入库状态：pending/processing/ready/failed
    private String status;

    // 分块数量
    private Integer chunkCount;

    // 文档版本
    private Integer version;

    // 入库失败原因
    private String errorMsg;

    // 创建时间
    private LocalDateTime createTime;

    // 按 seq 升序排列的分块，前端可以按段落展示
    private List<KbChunk> chunks;

    // 所有分块按顺序拼成的完整正文，方便一次性预览
    private String fullContent;
}
