package com.atp.server.agent.tool.impl;

import com.atp.agent.tool.AbstractAgentTool;
import com.atp.agent.tool.ToolContext;
import com.atp.agent.tool.ToolResult;
import com.atp.common.constant.StatusConstant;
import com.atp.common.exception.BaseException;
import com.atp.pojo.vo.ticket.TicketMessageVO;
import com.atp.server.service.TicketMessageService;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 查工单往来消息。只读。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TicketMessageListTool extends AbstractAgentTool {

    private final TicketMessageService messageService;

    /**
     * 工具编码，必须与 agent_tool.code 一致。
     *
     * @return 工具编码
     */
    @Override
    public String code() {
        return "ticket_message_list";
    }

    /**
     * 声明工具参数类型。
     *
     * @return 参数名到参数类型的映射
     */
    @Override
    public Map<String, Class<?>> paramTypes() {
        return Map.of(
                "ticketId", Long.class,
                "includeInternal", Boolean.class
        );
    }

    /**
     * 查询工单消息，并按参数过滤内部备注。
     *
     * @param args 工具参数
     * @param ctx 执行上下文
     * @return 消息列表及给模型转述的文本
     */
    @Override
    public ToolResult execute(Map<String, Object> args, ToolContext ctx) {
        Long ticketId = asLong(args, "ticketId");
        if (ticketId == null) {
            return ToolResult.fail("请提供工单 ID。");
        }

        boolean includeInternal = asBool(args, "includeInternal", false);
        try {
            List<TicketMessageVO> all = messageService.list(ticketId, ctx.userId());
            List<TicketMessageVO> visible = includeInternal
                    ? all
                    : all.stream()
                    .filter(message -> !StatusConstant.VISIBLE_INTERNAL.equals(message.getVisible()))
                    .toList();

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("ticketId", ticketId);
            data.put("includeInternal", includeInternal);
            data.put("count", visible.size());
            data.put("messages", visible);

            if (visible.isEmpty()) {
                return ToolResult.ok(data, "这个工单还没有往来消息。");
            }

            StringBuilder sb = new StringBuilder("工单 ").append(ticketId)
                    .append(" 共有 ").append(visible.size()).append(" 条消息：\n");
            for (TicketMessageVO message : visible) {
                sb.append("- ").append(message.getSenderName() == null ? message.getRole() : message.getSenderName())
                        .append(StatusConstant.VISIBLE_INTERNAL.equals(message.getVisible()) ? "（内部备注）" : "")
                        .append("：").append(message.getContent()).append("\n");
            }
            return ToolResult.ok(data, sb.toString());
        } catch (BaseException e) {
            return ToolResult.fail(e.getMessage());
        } catch (Exception e) {
            log.error("工具 ticket_message_list 执行失败", e);
            return ToolResult.fail("查询工单消息失败：" + e.getMessage());
        }
    }
}