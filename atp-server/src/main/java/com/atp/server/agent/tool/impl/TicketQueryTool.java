package com.atp.server.agent.tool.impl;

import com.atp.agent.tool.AbstractAgentTool;
import com.atp.agent.tool.ToolContext;
import com.atp.agent.tool.ToolResult;
import com.atp.common.result.PageResult;
import com.atp.pojo.query.TicketPageQueryDTO;
import com.atp.pojo.vo.ticket.TicketVO;
import com.atp.server.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 查工单列表。
 * 只读，可以重试。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TicketQueryTool extends AbstractAgentTool {

    private static final int PAGE_SIZE = 20;

    private final TicketService ticketService;

    /**
     * 工具编码，必须与 agent_tool.code 一致。
     *
     * @return 工具编码
     */
    @Override
    public String code() {
        return "ticket_query";
    }

    /**
     * 声明工具参数类型。
     *
     * @return 参数名到参数类型的映射
     */
    @Override
    public Map<String, Class<?>> paramTypes() {
        return Map.of(
                "status", String.class,
                "priority", String.class,
                "assigneeId", Long.class,
                "keyword", String.class
        );
    }

    /**
     * 按状态、优先级、处理人和关键词查询工单列表。
     *
     * @param args 工具参数
     * @param ctx 执行上下文
     * @return 工单列表及给模型转述的文本
     */
    @Override
    public ToolResult execute(Map<String, Object> args, ToolContext ctx) {
        try {
            TicketPageQueryDTO query = new TicketPageQueryDTO();
            query.setPage(1);
            query.setPageSize(PAGE_SIZE);

            String status = str(args, "status");
            if (status != null) {
                Integer parsed = parseStatus(status);
                if (parsed == null) {
                    return ToolResult.fail("无法识别状态「" + status
                            + "」，请用：待受理/处理中/已挂起/待用户确认/已解决/已关闭。");
                }
                query.setStatus(parsed);
            }

            String priority = str(args, "priority");
            if (priority != null) {
                Integer parsed = parsePriority(priority);
                if (parsed == null) {
                    return ToolResult.fail("无法识别优先级「" + priority + "」，请用：紧急/高/中/低。");
                }
                query.setPriority(parsed);
            }

            query.setAssigneeId(asLong(args, "assigneeId"));
            query.setKeyword(str(args, "keyword"));

            PageResult<TicketVO> page = ticketService.pageQuery(query, ctx.userId());
            if (page.getTotal() == 0) {
                return ToolResult.ok(List.of(), "没有查询到符合条件的工单。");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("共查询到 ").append(page.getTotal())
                    .append(" 条工单，以下是最多 ").append(page.getRecords().size()).append(" 条：\n");
            int i = 1;
            for (TicketVO ticket : page.getRecords()) {
                sb.append(i++).append(". [").append(ticket.getTicketNo()).append("] ")
                        .append(ticket.getTitle())
                        .append(" | 状态：").append(statusName(ticket.getStatus()))
                        .append(" | 优先级：").append(priorityName(ticket.getPriority()))
                        .append(" | 处理人：").append(ticket.getAssigneeName() == null ? "未指派" : ticket.getAssigneeName())
                        .append(" | SLA 剩余：").append(ticket.getSlaRemainMinutes() == null
                                ? "-" : ticket.getSlaRemainMinutes() + " 分钟")
                        .append("\n");
            }

            return ToolResult.ok(page.getRecords(), sb.toString());
        } catch (Exception e) {
            log.error("工具 ticket_query 执行失败", e);
            return ToolResult.fail("查询工单失败：" + e.getMessage());
        }
    }
}