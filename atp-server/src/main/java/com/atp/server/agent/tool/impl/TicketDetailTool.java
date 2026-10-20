package com.atp.server.agent.tool.impl;

import com.atp.agent.tool.AbstractAgentTool;
import com.atp.agent.tool.ToolContext;
import com.atp.agent.tool.ToolResult;
import com.atp.common.exception.BaseException;
import com.atp.pojo.vo.ticket.TicketDetailVO;
import com.atp.server.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 按 ID 查工单详情。只读。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TicketDetailTool extends AbstractAgentTool {

    private final TicketService ticketService;

    /**
     * 工具编码，必须与 agent_tool.code 一致。
     *
     * @return 工具编码
     */
    @Override
    public String code() {
        return "ticket_detail";
    }

    /**
     * 声明工具参数类型。
     *
     * @return 参数名到参数类型的映射
     */
    @Override
    public Map<String, Class<?>> paramTypes() {
        return Map.of("ticketId", Long.class);
    }

    /**
     * 查询指定工单的详情。
     *
     * @param args 工具参数
     * @param ctx 执行上下文
     * @return 工单详情及给模型转述的文本
     */
    @Override
    public ToolResult execute(Map<String, Object> args, ToolContext ctx) {
        Long ticketId = asLong(args, "ticketId");
        if (ticketId == null) {
            return ToolResult.fail("请提供工单 ID。如果你只记得标题，先用 ticket_query 按关键词查一下。");
        }
        try {
            TicketDetailVO detail = ticketService.detail(ticketId, ctx.userId());
            int messageCount = detail.getMessages() == null ? 0 : detail.getMessages().size();
            detail.setMessages(null);

            String text = "工单 [" + detail.getTicketNo() + "] " + detail.getTitle()
                    + "\n状态：" + statusName(detail.getStatus())
                    + "（可流转到：" + detail.getAllowedNextStatus() + "）"
                    + "\n优先级：" + priorityName(detail.getPriority())
                    + "\n分类：" + detail.getCategoryName()
                    + "\n提交人：" + detail.getRequesterName()
                    + "\n处理人：" + (detail.getAssigneeName() == null ? "未指派" : detail.getAssigneeName())
                    + "\nSLA 剩余：" + (detail.getSlaRemainMinutes() == null
                            ? "-" : detail.getSlaRemainMinutes() + " 分钟")
                    + "\n问题描述：" + detail.getContent()
                    + "\n往来消息：" + messageCount + " 条（要内容请调 ticket_message_list）";

            return ToolResult.ok(detail, text);
        } catch (BaseException e) {
            return ToolResult.fail(e.getMessage());
        } catch (Exception e) {
            log.error("工具 ticket_detail 执行失败", e);
            return ToolResult.fail("查询工单详情失败：" + e.getMessage());
        }
    }
}