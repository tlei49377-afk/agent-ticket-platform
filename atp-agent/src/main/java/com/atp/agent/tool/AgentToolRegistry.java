package com.atp.agent.tool;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具注册表。
 * Spring 启动时把所有 AgentTool 实现收集成 Map。
 */
@Component
public class AgentToolRegistry {

    private final Map<String, AgentTool> tools = new HashMap<>();

    /**
     * 收集 Spring 容器中的全部工具实现。
     *
     * @param toolList 工具实现列表
     */
    public AgentToolRegistry(List<AgentTool> toolList) {
        for (AgentTool tool : toolList) {
            tools.put(tool.code(), tool);
        }
    }

    /**
     * 按工具编码取工具。
     *
     * @param code 工具编码
     * @return 工具实现；不存在时返回 null
     */
    public AgentTool get(String code) {

        return tools.get(code);
    }

    /**
     * 返回全部已注册工具。
     *
     * @return 工具编码到工具实现的不可变副本
     */
    public Map<String, AgentTool> all() {

        return Map.copyOf(tools);
    }
}