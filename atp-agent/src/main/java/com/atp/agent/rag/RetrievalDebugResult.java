package com.atp.agent.rag;

import java.util.List;

/**
 * 检索调试结果，供测试接口和评测使用。
 *
 * @param mode 检索模式
 * @param results 最终结果
 * @param vectorHits 向量路结果
 * @param keywordHits 关键词路结果
 * @param costMs 总耗时
 */
public record RetrievalDebugResult(
        String mode,
        List<RetrievedChunk> results,
        List<ScoredChunk> vectorHits,
        List<ScoredChunk> keywordHits,
        long costMs
) {
}
