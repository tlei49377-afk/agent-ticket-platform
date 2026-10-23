package com.atp.agent.tool.impl;

import com.atp.agent.rag.HybridRetriever;
import com.atp.agent.rag.RetrievedChunk;
import com.atp.agent.tool.AbstractAgentTool;
import com.atp.agent.tool.ToolContext;
import com.atp.agent.tool.ToolResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 知识库混合检索工具。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class KbSearchTool extends AbstractAgentTool {

    /** 知识库检索器。 */
    private final HybridRetriever retriever;

    /**
     * 工具编码。
     *
     * @return 工具编码
     */
    @Override
    public String code() {
        return "kb_search";
    }

    /**
     * 声明工具参数类型。
     *
     * @return 参数类型
     */
    @Override
    public Map<String, Class<?>> paramTypes() {
        return Map.of("query", String.class, "topK", Integer.class);
    }

    /**
     * 执行知识库检索。
     *
     * @param args 工具参数
     * @param ctx 执行上下文
     * @return 召回结果
     */
    @Override
    public ToolResult execute(Map<String, Object> args, ToolContext ctx) {
        String query = str(args, "query");
        if (query == null) {
            return ToolResult.fail("请提供要检索的问题内容。");
        }
        Long topKValue = asLong(args, "topK");
        int topK = topKValue == null ? 5 : Math.max(1, Math.min(10, topKValue.intValue()));
        try {
            List<RetrievedChunk> chunks = retriever.retrieve(query, ctx.runId(), topK);
            if (chunks.isEmpty()) {
                return ToolResult.ok(List.of(), "知识库里没有找到相关资料。");
            }

            StringBuilder message = new StringBuilder("找到以下相关资料：\n");
            int index = 1;
            for (RetrievedChunk chunk : chunks) {
                message.append("[").append(index++).append("] 《")
                        .append(chunk.docTitle()).append("》\n")
                        .append(chunk.content()).append("\n\n");
            }
            return ToolResult.ok(chunks, message.toString());
        } catch (Exception e) {
            log.error("知识库检索失败", e);
            return ToolResult.fail("知识库暂时不可用，请稍后再试。");
        }
    }
}
