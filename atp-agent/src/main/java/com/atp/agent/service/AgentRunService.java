package com.atp.agent.service;

/**
 * Agent 运行记录服务。
 * 一次用户提问对应一条 agent_run。
 */
public interface AgentRunService {

    /**
     * 创建一次运行记录。
     *
     * @param memoryId 会话记忆 ID
     * @param userId 发起人 ID
     * @param message 用户输入
     * @return agent_run.id 的字符串形式，供工具上下文使用
     */
    String startRun(String memoryId, Long userId, String message);

    /**
     * 结束一次运行并写入最终状态、输出、耗时与错误信息。
     *
     * @param runId agent_run.id 的字符串形式
     * @param status 终态：success/failed/cancelled
     * @param outputText 最终输出，可空
     * @param latencyMs 总耗时毫秒
     * @param errorMsg 失败原因，成功时传 null
     */
    void finishRun(String runId,
                   String status,
                   String outputText,
                   long latencyMs,
                   String errorMsg);
}