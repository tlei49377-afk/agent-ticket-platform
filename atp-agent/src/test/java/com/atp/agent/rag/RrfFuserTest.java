package com.atp.agent.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * RRF 融合测试。
 */
class RrfFuserTest {

    /**
     * 两路共同命中的结果应排在最前。
     */
    @Test
    void fuse_共同命中应优先() {
        ScoredChunk a = chunk("a", 0.9);
        ScoredChunk b = chunk("b", 0.8);
        ScoredChunk c = chunk("c", 0.7);

        List<ScoredChunk> fused = RrfFuser.fuse(List.of(a, b), List.of(b, c));

        assertEquals("b", fused.get(0).id());
        assertEquals("a", fused.get(1).id());
        assertEquals("c", fused.get(2).id());
        assertEquals(3, fused.size());
    }

    /**
     * 构造测试分块。
     *
     * @param id 分块标识
     * @param score 原始分
     * @return 测试分块
     */
    private ScoredChunk chunk(String id, double score) {
        return new ScoredChunk(id, 1L, 1L, "标题", "内容", 1, score, 0.0);
    }
}
