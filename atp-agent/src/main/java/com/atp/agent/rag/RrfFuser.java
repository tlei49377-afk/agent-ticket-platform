package com.atp.agent.rag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reciprocal Rank Fusion - “互惠排名融合”。
 *
 */
public final class RrfFuser {

    /** RRF 平滑常数。 */
    private static final int K = 60;

    private RrfFuser() {
    }

    /**
     * 融合多路检索结果。
     *
     * @param routes 多路检索结果
     * @return 按 RRF 分数降序的结果
     */
    @SafeVarargs
    public static List<ScoredChunk> fuse(List<ScoredChunk>... routes) {
        Map<String, ScoredChunk> merged = new LinkedHashMap<>();
        Map<String, Double> scores = new LinkedHashMap<>();
        for (List<ScoredChunk> route : routes) {
            if (route == null) {
                continue;
            }
            int rank = 0;
            for (ScoredChunk chunk : route) {
                rank++;
                merged.putIfAbsent(chunk.id(), chunk);
                scores.merge(chunk.id(), 1.0 / (K + rank), Double::sum);
            }
        }

        List<ScoredChunk> fused = new ArrayList<>();
        for (ScoredChunk chunk : merged.values()) {
            fused.add(chunk.withRrfScore(scores.getOrDefault(chunk.id(), 0.0)));
        }
        fused.sort(Comparator.comparingDouble(ScoredChunk::rrfScore).reversed());
        return fused;
    }
}
