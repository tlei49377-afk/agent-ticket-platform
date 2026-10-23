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
 * FAQ 检索工具。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FaqSearchTool extends AbstractAgentTool {

    /** 知识库检索器。 */
    private final HybridRetriever retriever;

    /**
     * 工具编码。
     *
     * @return 工具编码
     */
    @Override
    public String code() {
        return "faq_search";
    }

    /**
     * 声明工具参数类型。
     *
     * @return 参数类型
     */
    @Override
    public Map<String, Class<?>> paramTypes() {
        return Map.of("question", String.class);
    }

    /**
     * 执行 FAQ 检索。
     *
     * @param args 工具参数
     * @param ctx 执行上下文
     * @return FAQ 召回结果
     */
    @Override
    public ToolResult execute(Map<String, Object> args, ToolContext ctx) {
        String question = str(args, "question");
        if (question == null) {
            return ToolResult.fail("请提供要查询的问题。");
        }
        try {
            List<RetrievedChunk> chunks = retriever.retrieveFaq(question, ctx.runId(), 3);
            if (chunks.isEmpty()) {
                return ToolResult.ok(List.of(), "没有匹配到常见问题。");
            }
            StringBuilder message = new StringBuilder("找到以下常见问题：\n");
            int index = 1;
            for (RetrievedChunk chunk : chunks) {
                message.append("[").append(index++).append("] ")
                        .append(chunk.content()).append("\n");
            }
            return ToolResult.ok(chunks, message.toString());
        } catch (Exception e) {
            log.error("FAQ 检索失败", e);
            return ToolResult.fail("FAQ 暂时不可用，请稍后再试。");
        }
    }
}
