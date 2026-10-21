package com.atp.agent.tool;

import com.atp.agent.mapper.AgentMapper;
import com.atp.agent.mapper.AgentSessionMapper;
import com.atp.common.utils.JsonUtils;
import com.atp.pojo.entity.agent.AgentMeta;
import com.atp.pojo.entity.agent.AgentSession;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 根据会话所属 Agent 解析工具授权范围。
 *
 * <p>agent.tool_scope 是 JSON 字符串数组。返回 null 表示没有配置范围，
 * 按 ToolExecutor 的约定视为不限制；配置存在但解析失败时返回空集合，
 * 采取拒绝策略，避免把未授权能力意外放开。</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AgentToolScopeResolver {

    /** JSON 数组反序列化类型。 */
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final AgentSessionMapper sessionMapper;
    private final AgentMapper agentMapper;

    /**
     * 解析 memoryId 对应 Agent 的可用工具集合。
     *
     * @param memoryId 会话记忆 ID
     * @return 工具编码集合；null 表示 Agent 未配置范围
     */
    public Set<String> resolve(String memoryId) {
        AgentSession session = sessionMapper.getByMemoryId(memoryId);
        if (session == null || session.getAgentId() == null) {
            log.warn("无法解析 Agent 工具范围：会话不存在或缺少 agentId，memoryId={}", memoryId);
            return Set.of();
        }

        AgentMeta agent = agentMapper.getById(session.getAgentId());
        if (agent == null) {
            log.warn("无法解析 Agent 工具范围：Agent 不存在，agentId={}", session.getAgentId());
            return Set.of();
        }

        String toolScope = agent.getToolScope();
        if (toolScope == null || toolScope.isBlank()) {
            return null;
        }

        try {
            List<String> codes = JsonUtils.fromJson(toolScope, STRING_LIST_TYPE);
            return codes == null ? Set.of() : new LinkedHashSet<>(codes);
        } catch (Exception e) {
            log.error("Agent {} 的 tool_scope 不是合法 JSON 数组：{}", agent.getId(), toolScope, e);
            return Set.of();
        }
    }
}