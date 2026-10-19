package com.atp.agent.tool;

/**
 * 工具执行上下文。显式传入，不依赖任何隐式状态。
 *
 */
public record ToolContext(
        Long userId,
        String username,
        String runId,
        Integer stepIndex,
        String traceId,
        String memoryId
) {

    /**
     * 从 memoryId（格式 userId:sessionId）里解析出调用者身份
     */
    public static Long parseUserId(String memoryId) {
        if (memoryId == null || !memoryId.contains(":")) {
            return null;
        }
        try {
            return Long.valueOf(memoryId.substring(0, memoryId.indexOf(':')));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String idempotentKey(String toolCode, String argsJson) {
        return runId + ":" + stepIndex + ":" + toolCode + ":"
                + Integer.toHexString(argsJson.hashCode());
    }
}
