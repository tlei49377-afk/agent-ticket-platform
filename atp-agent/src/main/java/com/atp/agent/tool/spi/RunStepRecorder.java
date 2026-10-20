package com.atp.agent.tool.spi;

import com.atp.agent.tool.ToolContext;

/**
 * 工具执行步骤落库。实现放 atp-server，写 agent_run_step。
 */
public interface RunStepRecorder {

    /**
     * 记一条工具执行步骤
     * @param ctx 执行上下文
     * @param toolCode 工具编码
     * @param status 步骤状态
     * @param detail 详情
     * @param costMs 耗时毫秒
     */
    void recordTool(ToolContext ctx, String toolCode, String status, String detail, long costMs);
}