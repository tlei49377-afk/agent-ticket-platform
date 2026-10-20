package com.atp.agent.tool;

import java.util.Map;

/**
 * 所有业务工具的接口。
 *
 * 实现类只要关注业务逻辑本身，
 * 身份/权限/幂等/超时/重试/审批/审计都由 ToolExecutor 统一处理。
 */
public interface AgentTool {

    /**
     * 工具编码，必须与 agent_tool.code 一致。
     *
     * @return 工具编码
     */
    String code();

    /**
     * 参数名与类型的声明，用于日志与校验。
     *
     * @return 参数名到参数类型的映射
     */
    Map<String, Class<?>> paramTypes();

    /**
     * 执行工具。args 的 key 与 @Tool 方法参数名一致。
     *
     * @param args 工具参数
     * @param ctx 执行上下文
     * @return 工具执行结果
     */
    ToolResult execute(Map<String, Object> args, ToolContext ctx);
}
