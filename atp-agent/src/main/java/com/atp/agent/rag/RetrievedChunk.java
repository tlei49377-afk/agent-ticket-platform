package com.atp.agent.rag;

/**
 * 一条知识库召回结果。
 *
 */
public record RetrievedChunk(
        Long chunkId, // 分块 ID
        Long documentId, // 文档 ID
        String docTitle, // 文档标题
        String content, // 分块正文
        double score, // 融合后的排序分
        Integer seq // 分块序号
) {

}
