package com.atp.agent.tool.spi;

import com.atp.agent.tool.ToolContext;

import java.util.Map;

/**
 * 工具审计。实现放 atp-server，写 sys_audit_log。
 */
public interface AuditService {

    /**
     * 记一条工具调用审计
     * @param ctx 执行上下文
     * @param toolCode 工具编码
     * @param args 调用参数
     * @param success 是否成功
     * @param detail 详情，通常是失败原因或"待审批"
     * @param costMs 耗时毫秒
     */
    void recordTool(ToolContext ctx, String toolCode, Map<String, Object> args,
                    boolean success, String detail, long costMs);
}