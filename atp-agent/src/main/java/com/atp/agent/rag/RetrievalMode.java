package com.atp.agent.rag;

import java.util.Locale;

/**
 * 知识库检索模式。
 */
public enum RetrievalMode {

    /** 向量检索, 关键词检索, 向量与关键词 RRF 融合。 */
    VECTOR, KEYWORD, HYBRID;

    /**
     * 解析检索模式。
     *
     * @param value vector/keyword/hybrid
     * @return 检索模式，默认 HYBRID
     */
    public static RetrievalMode from(String value) {
        if (value == null || value.isBlank()) {
            return HYBRID;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "vector" -> VECTOR;
            case "keyword" -> KEYWORD;
            case "hybrid" -> HYBRID;
            default -> throw new IllegalArgumentException("不支持的检索模式：" + value);
        };
    }

    /**
     * 返回小写模式名。
     *
     * @return vector/keyword/hybrid
     */
    public String code() {

        return name().toLowerCase(Locale.ROOT);
    }
}
