package com.atp.agent.tool;

/**
 * 工具执行的统一返回。
 *
 * data 给程序用（评测、前端表格、二次加工）;
 * message 给模型用（自然语言，模型转述给用户）。
 *
 * @param success 是否执行成功
 * @param data 结构化结果，供程序消费
 * @param message 自然语言结果，供模型转述
 * @param needApproval 是否需要人工审批
 * @param approvalReason 需要审批时给出的原因
 */
public record ToolResult(
        boolean success,
        Object data,
        String message,
        boolean needApproval,
        String approvalReason
) {

    /**
     * 构造一条执行成功的结果。
     *
     * @param data 结构化结果
     * @param message 自然语言提示
     * @return 成功结果
     */
    public static ToolResult ok(Object data, String message) {
        return new ToolResult(true, data, message, false, null);
    }

    /**
     * 构造一条执行失败的结果。
     *
     * @param message 失败原因
     * @return 失败结果
     */
    public static ToolResult fail(String message) {
        return new ToolResult(false, null, message, false, null);
    }

    /**
     * 构造一条等待人工审批的结果。
     *
     * @param reason 审批原因
     * @param data 已整理的结构化参数
     * @return 待审批结果
     */
    public static ToolResult needApproval(String reason, Object data) {
        return new ToolResult(true, data, "该操作需要人工审批", true, reason);
    }
}