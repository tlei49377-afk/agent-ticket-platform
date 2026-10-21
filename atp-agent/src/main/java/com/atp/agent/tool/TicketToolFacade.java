package com.atp.agent.tool;

import com.atp.agent.service.ApprovalService;
import com.atp.agent.tool.spi.UsernameResolver;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 暴露给模型的工具门面。
 *
 * 每个 {@link Tool} 方法只负责收集参数、解析 memoryId 中的调用者身份，
 * 然后交给 {@link ToolExecutor} 统一完成鉴权、幂等、审批、超时、重试与审计。
 * 业务逻辑全部放在 atp-server 的 {@link AgentTool} 实现中。
 *
 * 工具执行可能发生在模型返回 tool_calls 后的其他线程，因此身份不能依赖
 * ThreadLocal。这里使用 {@link ToolMemoryId} 让框架注入
 * {@code userId:sessionId} 格式的 memoryId，再从中解析用户 ID。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TicketToolFacade {

    // 统一工具执行器。
    private final ToolExecutor executor;

    // 用户展示名解析，避免 atp-agent 反向依赖 atp-server。
    private final UsernameResolver usernameResolver;

    // 会话所属 Agent 的工具授权范围解析器。
    private final AgentToolScopeResolver scopeResolver;

    // 当前会话 run 上下文存储。
    private final AgentRunContextStore runContextStore;

    // 写操作审批单服务。
    private final ApprovalService approvalService;

    /**
     * 查询工单列表。
     *
     * @param memoryId 会话记忆 ID
     * @param status 工单状态，可为空
     * @param priority 优先级，可为空
     * @param assigneeId 处理人用户 ID，可为空；姓名需先通过 user_query 转换
     * @param keyword 标题或工单号关键词，可为空
     * @return 给模型转述的查询结果
     */
    @Tool(name = "ticket_query", value = "查询工单列表。可以根据状态、优先级、处理人用户 ID、"
            + "标题或工单号关键词查询，返回工单号、标题、状态、优先级、处理人和创建时间。"
            + "当用户询问有哪些工单、某人的工单或待处理工单时调用。")
    public String ticketQuery(
            @ToolMemoryId String memoryId,
            @P(value = "工单状态：待受理/处理中/已挂起/待用户确认/已解决/已关闭；没提时传空字符串", required = false)
            String status,
            @P(value = "优先级：紧急/高/中/低；没提时传空字符串", required = false)
            String priority,
            @P(value = "处理人用户 ID；若用户只提供姓名，先调用 user_query 查出 ID；没提时传 null", required = false)
            Long assigneeId,
            @P(value = "匹配工单标题或工单号的关键词；没提时传空字符串", required = false)
            String keyword) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("status", status);
        args.put("priority", priority);
        args.put("assigneeId", assigneeId);
        args.put("keyword", keyword);
        return dispatch("ticket_query", args, memoryId);
    }

    /**
     * 查询单张工单详情。
     *
     * @param memoryId 会话记忆 ID
     * @param ticketId 工单 ID
     * @return 给模型转述的工单详情
     */
    @Tool(name = "ticket_detail", value = "按工单 ID 查询详情，包括标题、状态、优先级、处理人、"
            + "SLA 与可流转状态。只知道标题或工单号时，先用 ticket_query 找到工单 ID。")
    public String ticketDetail(
            @ToolMemoryId String memoryId,
            @P(value = "工单 ID", required = true) Long ticketId) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("ticketId", ticketId);
        return dispatch("ticket_detail", args, memoryId);
    }

    /**
     * 查询工单往来消息。
     *
     * @param memoryId 会话记忆 ID
     * @param ticketId 工单 ID
     * @param includeInternal 是否包含内部备注
     * @return 给模型转述的消息列表
     */
    @Tool(name = "ticket_message_list", value = "查询工单往来消息。默认不返回内部备注；"
            + "只有当前用户明确要查看内部备注时才把 includeInternal 设为 true。")
    public String ticketMessageList(
            @ToolMemoryId String memoryId,
            @P(value = "工单 ID", required = true) Long ticketId,
            @P(value = "是否包含仅内部可见的备注，默认 false", required = false)
            Boolean includeInternal) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("ticketId", ticketId);
        args.put("includeInternal", Boolean.TRUE.equals(includeInternal));
        return dispatch("ticket_message_list", args, memoryId);
    }

    /**
     * 查询用户。
     *
     * @param memoryId 会话记忆 ID
     * @param keyword 姓名或登录名关键词，可为空
     * @param role 角色编码，可为空
     * @return 给模型转述的用户列表
     */
    @Tool(name = "user_query", value = "按姓名、登录名或角色查询用户。处理人姓名转换成用户 ID、"
            + "核实用户身份时调用；匹配多个用户时必须让用户确认具体是哪一位。")
    public String userQuery(
            @ToolMemoryId String memoryId,
            @P(value = "姓名或登录名关键词；没提时传空字符串", required = false) String keyword,
            @P(value = "角色编码，如客服或主管；没提时传空字符串", required = false) String role) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("keyword", keyword);
        args.put("role", role);
        return dispatch("user_query", args, memoryId);
    }

    /**
     * 查询 SLA 状态。
     *
     * @param memoryId 会话记忆 ID
     * @param ticketId 工单 ID；为空时查询近期即将超期的工单
     * @return 给模型转述的 SLA 状态
     */
    @Tool(name = "sla_status_query", value = "查询工单 SLA 剩余时间或超期情况。传 ticketId 查询指定工单；"
            + "不传时查询近期即将超期的工单。")
    public String slaStatusQuery(
            @ToolMemoryId String memoryId,
            @P(value = "工单 ID；想查询整体即将超期情况时不传或传 null", required = false)
            Long ticketId) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("ticketId", ticketId);
        return dispatch("sla_status_query", args, memoryId);
    }

    /**
     * 查询工单统计指标。
     *
     * @param memoryId 会话记忆 ID
     * @param dimension 统计维度：日、周、月
     * @return 给模型转述的统计结果
     */
    @Tool(name = "stats_query", value = "按日、周或月查询工单统计指标，包括总量、解决率、超期数和平均首次响应时间。")
    public String statsQuery(
            @ToolMemoryId String memoryId,
            @P(value = "统计维度：日/周/月；没提时传日", required = false) String dimension) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("dimension", dimension);
        return dispatch("stats_query", args, memoryId);
    }

    /**
     * 指派或转派工单。
     *
     * @param memoryId 会话记忆 ID
     * @param ticketId 工单 ID
     * @param assigneeId 接手人的用户 ID
     * @param reason 转派原因，可为空
     * @return 给模型转述的指派结果
     */
    @Tool(name = "ticket_assign", value = "把工单指派或转派给指定客服。这是写操作，调用前必须先向用户复述"
            + "工单、接手人和原因，并取得用户明确同意；不知道用户 ID 时先调用 user_query。")
    public String ticketAssign(
            @ToolMemoryId String memoryId,
            @P(value = "工单 ID", required = true) Long ticketId,
            @P(value = "接手人的用户 ID；姓名需先用 user_query 转换", required = true) Long assigneeId,
            @P(value = "指派或转派原因；没提时传空字符串", required = false) String reason) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("ticketId", ticketId);
        args.put("assigneeId", assigneeId);
        args.put("reason", reason);
        return dispatch("ticket_assign", args, memoryId);
    }

    /**
     * 创建工单。
     *
     * @param memoryId 会话记忆 ID
     * @param title 工单标题
     * @param content 问题描述
     * @param categoryId 分类 ID，可为空
     * @param priority 优先级
     * @param requesterId 提交人用户 ID；当前版本只支持本人
     * @return 给模型转述的建单结果
     */
    @Tool(name = "ticket_create", value = "创建一张 AI 渠道工单。这是写操作，调用前必须先向用户复述标题、"
            + "问题描述、分类和优先级，并取得用户明确同意。")
    public String ticketCreate(
            @ToolMemoryId String memoryId,
            @P(value = "工单标题，简洁概括问题", required = true) String title,
            @P(value = "用户问题的完整描述", required = true) String content,
            @P(value = "工单分类 ID；用户没指定时传 null", required = false) Long categoryId,
            @P(value = "优先级：紧急/高/中/低", required = true) String priority,
            @P(value = "提交人用户 ID；当前版本只能填写当前用户，不填也可", required = false)
            Long requesterId) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("title", title);
        args.put("content", content);
        args.put("categoryId", categoryId);
        args.put("priority", priority);
        args.put("requesterId", requesterId);
        return dispatch("ticket_create", args, memoryId);
    }

    /**
     * 升级工单优先级。
     *
     * @param memoryId 会话记忆 ID
     * @param ticketId 工单 ID
     * @param targetPriority 目标优先级
     * @param reason 升级原因
     * @return 给模型转述的升级结果
     */
    @Tool(name = "ticket_escalate", value = "升级工单优先级，只允许变得更紧急。这是写操作，调用前必须先向用户"
            + "复述工单、目标优先级和原因，并取得用户明确同意。")
    public String ticketEscalate(
            @ToolMemoryId String memoryId,
            @P(value = "工单 ID", required = true) Long ticketId,
            @P(value = "目标优先级：紧急/高/中/低", required = true) String targetPriority,
            @P(value = "升级原因，必须说明", required = true) String reason) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("ticketId", ticketId);
        args.put("targetPriority", targetPriority);
        args.put("reason", reason);
        return dispatch("ticket_escalate", args, memoryId);
    }

    /**
     * 回复工单。
     *
     * @param memoryId 会话记忆 ID
     * @param ticketId 工单 ID
     * @param content 回复内容
     * @param visible 可见性：public 或 internal
     * @return 给模型转述的回复结果
     */
    @Tool(name = "ticket_reply", value = "回复工单或添加内部备注。这是写操作，调用前必须先向用户复述回复内容"
            + "和可见性，并取得用户明确同意。visible 只能是 public（客户可见）或 internal（仅内部可见）。")
    public String ticketReply(
            @ToolMemoryId String memoryId,
            @P(value = "工单 ID", required = true) Long ticketId,
            @P(value = "回复正文", required = true) String content,
            @P(value = "可见性：public 客户可见 / internal 仅内部可见", required = true) String visible) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("ticketId", ticketId);
        args.put("content", content);
        args.put("visible", visible);
        return dispatch("ticket_reply", args, memoryId);
    }

    /**
     * 流转工单状态。
     *
     * @param memoryId 会话记忆 ID
     * @param ticketId 工单 ID
     * @param targetStatus 目标状态
     * @param reason 流转原因，关闭工单时必填
     * @return 给模型转述的状态流转结果
     */
    @Tool(name = "ticket_update_status", value = "流转工单状态。这是写操作，调用前必须先向用户复述工单、"
            + "当前状态、目标状态和原因，并取得用户明确同意。关闭工单不可逆，必须单独确认并填写原因。")
    public String ticketUpdateStatus(
            @ToolMemoryId String memoryId,
            @P(value = "工单 ID", required = true) Long ticketId,
            @P(value = "目标状态：待受理/处理中/已挂起/待用户确认/已解决/已关闭", required = true)
            String targetStatus,
            @P(value = "状态流转原因；关闭工单时必填", required = false) String reason) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("ticketId", ticketId);
        args.put("targetStatus", targetStatus);
        args.put("reason", reason);
        return dispatch("ticket_update_status", args, memoryId);
    }

    /**
     * 解析调用身份和 run 上下文，并交给统一执行器。
     *
     * @param toolCode 工具编码
     * @param args 工具参数
     * @param memoryId 会话记忆 ID
     * @return 给模型转述的执行结果
     */
    private String dispatch(String toolCode, Map<String, Object> args, String memoryId) {
        Long userId = ToolContext.parseUserId(memoryId);
        if (userId == null) {
            log.error("memoryId 格式非法，无法解析用户身份：{}", memoryId);
            return "会话信息异常，请重新登录后再试。";
        }

        AgentRunContextStore.RunContext runContext = runContextStore.current(memoryId).orElse(null);
        String runId = runContext == null ? null : runContext.runId();
        String traceId = runContext == null ? null : runContext.traceId();
        int step = runContextStore.nextStep(runId);

        ToolContext ctx = new ToolContext(
                userId,
                usernameResolver.getUsername(userId),
                runId,
                step,
                traceId,
                memoryId);

        ToolResult result = executor.execute(
                toolCode,
                args,
                ctx,
                scopeResolver.resolve(memoryId));

        if (result.needApproval()) {
            try {
                Long approvalId = approvalService.createPending(
                        runId, memoryId, step, toolCode, args, result.approvalReason());
                if (approvalId != null) {
                    return result.message() + "，审批单号：#" + approvalId
                            + "。请主管在审批中心处理，不要重复调用该工具。";
                }
            } catch (Exception e) {
                log.error("创建工具审批单失败：toolCode={}, runId={}", toolCode, runId, e);
                return "该操作需要审批，但审批单创建失败，请稍后重试。";
            }
        }
        return result.message();
    }
}
