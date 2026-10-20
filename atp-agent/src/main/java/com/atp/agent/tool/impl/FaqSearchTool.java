package com.atp.agent.tool.impl;

import com.atp.agent.tool.AbstractAgentTool;
import com.atp.agent.tool.ToolContext;
import com.atp.agent.tool.ToolResult;
import com.atp.common.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 常见问题匹配。占位。
 */
@Component
@Slf4j
public class FaqSearchTool extends AbstractAgentTool {

    /**
     * 工具编码，必须与 agent_tool.code 一致。
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
     * @return 参数名到参数类型的映射
     */
    @Override
    public Map<String, Class<?>> paramTypes() {
        return Map.of("question", String.class);
    }

    /**
     * 占位，接入知识库后再补齐匹配逻辑。
     *
     * @param args 工具参数
     * @param ctx 执行上下文
     * @return 本阶段不会返回正常结果
     */
    @Override
    public ToolResult execute(Map<String, Object> args, ToolContext ctx) {
        throw new BaseException("知识库未接入");
    }
}