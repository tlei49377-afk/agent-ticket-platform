package com.atp.agent.tool;

/**
 * 工具执行的统一返回。
 *
 * data 给程序用（评测、前端表格、二次加工），
 * message 给模型用（自然语言，模型转述给用户）。
 * 分开设计是因为项目一吃过亏：只返回格式化文本，程序没法消费。
 */
public record ToolResult(
        boolean success,
        Object data,
        String message,
        boolean needApproval,
        String approvalReason
) {

    public static ToolResult ok(Object data, String message) {
        return new ToolResult(true, data, message, false, null);
    }

    public static ToolResult fail(String message) {
        return new ToolResult(false, null, message, false, null);
    }

    public static ToolResult needApproval(String reason, Object data) {
        return new ToolResult(true, data, "该操作需要人工审批", true, reason);
    }
}
