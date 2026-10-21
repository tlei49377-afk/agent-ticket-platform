package com.atp.agent.tool;

/**
 * 工具执行上下文。显式传入，不依赖任何隐式状态。
 *
 * @param userId 当前用户 ID
 * @param username 当前用户名
 * @param runId 本轮运行 ID
 * @param stepIndex 当前步骤序号
 * @param traceId 链路追踪 ID
 * @param memoryId 会话记忆 ID，格式为 userId:sessionId
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
     * 从 memoryId（格式 userId:sessionId）里解析出调用者身份。
     *
     * @param memoryId 会话记忆 ID
     * @return 用户 ID；格式不合法时返回 null
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

    /**
     * 生成一次工具调用的幂等键。
     *
     * @param toolCode 工具编码
     * @param argsJson 参数 JSON
     * @return 幂等键
     */
    public String idempotentKey(String toolCode, String argsJson) {
        String safeJson = argsJson == null ? "" : argsJson;
        return runId + ":" + stepIndex + ":" + toolCode + ":" + sha256(safeJson);
    }

    /**
     * 计算参数 JSON 的 SHA-256，避免 String.hashCode 碰撞导致错误去重。
     *
     * @param text 待计算文本
     * @return 十六进制摘要
     */
    private static String sha256(String text) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
