package com.atp.server.service.impl;

import com.atp.agent.mapper.KbChunkMapper;
import com.atp.agent.mapper.KbDocumentMapper;
import com.atp.agent.mapper.KbRetrievalLogMapper;
import com.atp.common.utils.JsonUtils;
import com.atp.pojo.entity.kb.KbChunk;
import com.atp.pojo.entity.kb.KbDocument;
import com.atp.pojo.entity.kb.KbRetrievalLog;
import com.atp.pojo.vo.kb.KbReferenceVO;
import com.atp.server.service.KbReferenceService;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 知识库引用查询实现。
 */
@Service
@RequiredArgsConstructor
public class KbReferenceServiceImpl implements KbReferenceService {

    // 分块 ID 列表反序列化类型
    private static final TypeReference<List<Long>> LONG_LIST = new TypeReference<>() {
    };

    // 分数列表反序列化类型
    private static final TypeReference<List<Double>> DOUBLE_LIST = new TypeReference<>() {
    };

    private final KbRetrievalLogMapper retrievalLogMapper;
    private final KbChunkMapper chunkMapper;
    private final KbDocumentMapper documentMapper;

    /**
     * 查询一次运行最近一次检索的引用。
     *
     * @param runId 运行 ID
     * @return 引用列表
     */
    @Override
    public List<KbReferenceVO> listByRun(Long runId) {
        KbRetrievalLog log = retrievalLogMapper.getLatestByRun(runId);
        if (log == null || log.getResultIds() == null || log.getResultIds().isBlank()) {
            return List.of();
        }
        List<Long> chunkIds = JsonUtils.fromJson(log.getResultIds(), LONG_LIST).stream()
                .filter(id -> id != null)
                .toList();
        if (chunkIds.isEmpty()) {
            return List.of();
        }

        List<Double> scores = log.getResultScores() == null || log.getResultScores().isBlank()
                ? List.of() : JsonUtils.fromJson(log.getResultScores(), DOUBLE_LIST);
        Map<Long, KbChunk> chunkMap = chunkMapper.listByIds(chunkIds).stream()
                .collect(Collectors.toMap(KbChunk::getId, Function.identity(), (left, right) -> left));
        Map<Long, KbDocument> documentMap = chunkMap.values().stream()
                .map(KbChunk::getDocumentId)
                .distinct()
                .map(documentMapper::getById)
                .filter(document -> document != null)
                .collect(Collectors.toMap(KbDocument::getId, Function.identity()));

        List<KbReferenceVO> references = new ArrayList<>();
        for (int i = 0; i < chunkIds.size(); i++) {
            KbChunk chunk = chunkMap.get(chunkIds.get(i));
            if (chunk == null) {
                continue;
            }
            KbDocument document = documentMap.get(chunk.getDocumentId());
            KbReferenceVO reference = new KbReferenceVO();
            reference.setIndex(i + 1);
            reference.setChunkId(chunk.getId());
            reference.setDocumentId(chunk.getDocumentId());
            reference.setDocTitle(document == null ? "未知文档" : document.getTitle());
            reference.setSnippet(chunk.getContent());
            reference.setScore(i < scores.size() ? scores.get(i) : null);
            references.add(reference);
        }
        return references;
    }
}
