package com.atp.agent.rag;

/**
 * 单路检索的中间结果。
 *
 * @param id 分块唯一标识，优先使用向量库主键
 * @param chunkId 数据库分块 ID
 * @param documentId 文档 ID
 * @param docTitle 文档标题
 * @param content 分块正文
 * @param seq 分块序号
 * @param rawScore 单路原始分数
 * @param rrfScore RRF 融合分数
 */
public record ScoredChunk(
        String id,
        Long chunkId,
        Long documentId,
        String docTitle,
        String content,
        Integer seq,
        double rawScore,
        double rrfScore
) {

    /**
     * 生成带 RRF 分数的新对象。
     *
     * @param score RRF 分数
     * @return 新对象
     */
    public ScoredChunk withRrfScore(double score) {
        return new ScoredChunk(id, chunkId, documentId, docTitle, content, seq, rawScore, score);
    }
}
