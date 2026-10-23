package com.atp.agent.rag;

import com.atp.agent.mapper.KbChunkMapper;
import com.atp.agent.mapper.KbRetrievalLogMapper;
import com.atp.common.utils.JsonUtils;
import com.atp.pojo.entity.kb.KbChunk;
import com.atp.pojo.entity.kb.KbRetrievalLog;
import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.search.Document;
import redis.clients.jedis.search.Query;
import redis.clients.jedis.search.SearchResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 知识库混合检索器。
 *
 * 向量路使用 LangChain4j 的 EmbeddingStore，关键词路使用 Redis Stack
 * 的 RediSearch JSON 索引，最终通过 RRF 按排名融合
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class HybridRetriever {

    /** LangChain4j 自动创建的索引名。 */
    private static final String INDEX = "embedding-index";

    /** 每路检索条数。 */
    private static final int TOP_K_PER_ROUTE = 10;

    /** 向量路最低相似度，先设为 0，避免误过滤掉有效结果。 */
    private static final double MIN_VECTOR_SCORE = 0.0;

    /** 最大返回条数。 */
    private static final int MAX_TOP_K = 20;

    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final ObjectProvider<EmbeddingStore<TextSegment>> embeddingStoreProvider;
    private final KbChunkMapper chunkMapper;
    private final KbRetrievalLogMapper logMapper;
    private final JedisPooled vectorJedis;

    /**
     * 执行默认混合检索。
     *
     * @param query 用户问题
     * @param runId 当前运行 ID
     * @param topK 最终返回条数
     * @return 融合后的分块
     */
    public List<RetrievedChunk> retrieve(String query, String runId, int topK) {
        return retrieveDebug(query, runId, topK, RetrievalMode.HYBRID).results();
    }

    /**
     * 执行 FAQ 检索。
     *
     * <p>当前 FAQ 尚未单独建库，先复用知识库检索；后续可按 FAQ 索引单独实现。</p>
     *
     * @param question 用户问题
     * @param runId 当前运行 ID
     * @param topK 返回条数
     * @return FAQ 召回结果
     */
    public List<RetrievedChunk> retrieveFaq(String question, String runId, int topK) {
        return retrieve(question, runId, topK);
    }

    /**
     * 执行可观察的检索，返回各路结果和最终排序。
     *
     * @param query 用户问题
     * @param runId 当前运行 ID，可为空
     * @param topK 最终返回条数
     * @param mode 检索模式
     * @return 检索调试结果
     */
    public RetrievalDebugResult retrieveDebug(String query, String runId, int topK, RetrievalMode mode) {
        if (!StringUtils.hasText(query)) {
            return new RetrievalDebugResult(mode.code(), List.of(), List.of(), List.of(), 0L);
        }

        int safeTopK = Math.max(1, Math.min(MAX_TOP_K, topK));
        long start = System.currentTimeMillis();
        List<ScoredChunk> vectorHits = mode == RetrievalMode.KEYWORD
                ? List.of() : vectorSearch(query, Math.max(safeTopK, TOP_K_PER_ROUTE));
        List<ScoredChunk> keywordHits = mode == RetrievalMode.VECTOR
                ? List.of() : keywordSearch(query, Math.max(safeTopK, TOP_K_PER_ROUTE));

        List<RetrievedChunk> results = switch (mode) {
            case VECTOR -> vectorHits.stream()
                    .limit(safeTopK)
                    .map(chunk -> toChunk(chunk, false))
                    .toList();
            case KEYWORD -> keywordHits.stream()
                    .limit(safeTopK)
                    .map(chunk -> toChunk(chunk, false))
                    .toList();
            case HYBRID -> RrfFuser.fuse(vectorHits, keywordHits).stream()
                    .limit(safeTopK)
                    .map(chunk -> toChunk(chunk, true))
                    .toList();
        };

        long costMs = System.currentTimeMillis() - start;
        try {
            logMapper.insert(buildLog(query, runId, mode, safeTopK, results, costMs));
        } catch (Exception e) {
            log.warn("写入知识库检索日志失败，检索结果不受影响：{}", e.getMessage());
        }
        log.info("知识库检索完成：mode={}, vector={}, keyword={}, result={}, costMs={}",
                mode.code(), vectorHits.size(), keywordHits.size(), results.size(), costMs);
        return new RetrievalDebugResult(mode.code(), results, vectorHits, keywordHits, costMs);
    }

    /**
     * 向量路检索。
     *
     * @param query 查询文本
     * @param topK 返回条数
     * @return 向量检索结果
     */
    private List<ScoredChunk> vectorSearch(String query, int topK) {
        EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
        EmbeddingStore<TextSegment> embeddingStore = embeddingStoreProvider.getIfAvailable();
        if (embeddingModel == null || embeddingStore == null) {
            log.warn("知识库向量库未启用，向量路返回空结果");
            return List.of();
        }

        Embedding embedding = embeddingModel.embed(query).content();
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(embedding)
                .maxResults(topK)
                .minScore(MIN_VECTOR_SCORE)
                .build();
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(request).matches();
        Map<String, KbChunk> chunks = loadChunksByVectorIds(
                matches.stream().map(EmbeddingMatch::embeddingId).toList());

        List<ScoredChunk> results = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : matches) {
            TextSegment segment = match.embedded();
            KbChunk chunk = chunks.get(match.embeddingId());
            results.add(new ScoredChunk(
                    match.embeddingId(),
                    chunk == null ? null : chunk.getId(),
                    metadataLong(segment, "document_id", chunk == null ? null : chunk.getDocumentId()),
                    metadataString(segment, "doc_title", "未知文档"),
                    segment == null ? "" : segment.text(),
                    metadataInteger(segment, "seq", chunk == null ? null : chunk.getSeq()),
                    match.score(),
                    0.0));
        }
        return results;
    }

    /**
     * 关键词路检索。
     *
     * @param query 查询文本
     * @param topK 返回条数
     * @return 关键词检索结果
     */
    private List<ScoredChunk> keywordSearch(String query, int topK) {
        String keywordQuery = buildKeywordQuery(query);
        if (keywordQuery == null) {
            return List.of();
        }
        try {
            Query searchQuery = new Query(keywordQuery)
                    .limit(0, topK)
                    .setWithScores()
                    .returnFields("$");
            SearchResult searchResult = vectorJedis.ftSearch(INDEX, searchQuery);
            List<Document> documents = searchResult.getDocuments();
            Map<String, KbChunk> chunks = loadChunksByVectorIds(
                    documents.stream().map(Document::getId).toList());

            List<ScoredChunk> results = new ArrayList<>();
            for (Document document : documents) {
                JsonNode json = JsonUtils.mapper().readTree(document.getString("$"));
                KbChunk chunk = chunks.get(document.getId());
                results.add(new ScoredChunk(
                        document.getId(),
                        chunk == null ? null : chunk.getId(),
                        json.path("document_id").asLong(0L),
                        json.path("doc_title").asText(""),
                        json.path("text").asText(""),
                        json.path("seq").asInt(0),
                        document.getScore() == null ? 0.0 : document.getScore(),
                        0.0));
            }
            return results;
        } catch (Exception e) {
            log.warn("知识库关键词检索失败，降级为向量结果：{}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 构造 RediSearch 关键词查询。
     *
     * @param query 用户问题
     * @return 查询表达式；无有效关键词时返回 null
     */
    private String buildKeywordQuery(String query) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : normalized.split("[\\s\\p{Punct}]+")) {
            String safe = token.replaceAll("[^\\p{IsHan}\\p{L}\\p{N}_]", "");
            if (!safe.isBlank()) {
                tokens.add(safe);
            }
        }
        if (tokens.isEmpty()) {
            return null;
        }
        return "@text:(" + String.join("|", tokens) + ")";
    }

    /**
     * 按向量主键批量加载分块。
     *
     * @param vectorIds 向量主键
     * @return 向量主键到分块的映射
     */
    private Map<String, KbChunk> loadChunksByVectorIds(List<String> vectorIds) {
        if (vectorIds == null || vectorIds.isEmpty()) {
            return Map.of();
        }
        return chunkMapper.listByVectorIds(vectorIds).stream()
                .collect(Collectors.toMap(KbChunk::getVectorId, chunk -> chunk, (left, right) -> left));
    }

    /**
     * 将中间结果转成对外结果。
     *
     * @param chunk 中间结果
     * @param fused 是否使用 RRF 分数
     * @return 召回结果
     */
    private RetrievedChunk toChunk(ScoredChunk chunk, boolean fused) {
        return new RetrievedChunk(chunk.chunkId(), chunk.documentId(), chunk.docTitle(),
                chunk.content(), fused ? chunk.rrfScore() : chunk.rawScore(), chunk.seq());
    }

    /**
     * 构造检索日志。
     *
     * @param query 查询文本
     * @param runId 运行 ID
     * @param mode 检索模式
     * @param topK 返回条数
     * @param result 检索结果
     * @param costMs 耗时
     * @return 检索日志
     */
    private KbRetrievalLog buildLog(String query, String runId, RetrievalMode mode,
                                    int topK, List<RetrievedChunk> result, long costMs) {
        KbRetrievalLog log = new KbRetrievalLog();
        log.setQueryText(query.length() > 500 ? query.substring(0, 500) : query);
        log.setRunId(parseLong(runId));
        log.setMode(mode.code());
        log.setTopK(topK);
        log.setResultIds(JsonUtils.toJson(result.stream().map(RetrievedChunk::chunkId).toList()));
        log.setResultScores(JsonUtils.toJson(result.stream().map(RetrievedChunk::score).toList()));
        log.setTop1Score(result.isEmpty() ? null : BigDecimal.valueOf(result.get(0).score()));
        log.setCostMs((int) Math.min(Integer.MAX_VALUE, costMs));
        return log;
    }

    /**
     * 解析字符串 ID。
     *
     * @param value 字符串 ID
     * @return Long；非法时返回 null
     */
    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long metadataLong(TextSegment segment, String key, Long defaultValue) {
        if (segment == null || !segment.metadata().containsKey(key)) {
            return defaultValue;
        }
        return segment.metadata().getLong(key);
    }

    private Integer metadataInteger(TextSegment segment, String key, Integer defaultValue) {
        if (segment == null || !segment.metadata().containsKey(key)) {
            return defaultValue;
        }
        return segment.metadata().getInteger(key);
    }

    private String metadataString(TextSegment segment, String key, String defaultValue) {
        if (segment == null || !segment.metadata().containsKey(key)) {
            return defaultValue;
        }
        String value = segment.metadata().getString(key);
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
