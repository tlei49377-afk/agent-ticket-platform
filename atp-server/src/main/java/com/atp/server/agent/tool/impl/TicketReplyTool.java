package com.atp.server.agent.tool.impl;

import com.atp.agent.tool.AbstractAgentTool;
import com.atp.agent.tool.ToolContext;
import com.atp.agent.tool.ToolResult;
import com.atp.common.constant.StatusConstant;
import com.atp.common.exception.BaseException;
import com.atp.pojo.dto.ticket.TicketReplyDTO;
import com.atp.server.service.TicketMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 添加工单回复。
 * 写操作，支持 public 和 internal 两种可见性。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TicketReplyTool extends AbstractAgentTool {

    // 工单消息服务
    private final TicketMessageService messageService;

    /**
     * 工具编码，必须与 agent_tool.code 一致。
     *
     * @return 工具编码
     */
    @Override
    public String code() {
        return "ticket_reply";
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
                "content", String.class,
                "visible", String.class
        );
    }

    /**
     * 给工单添加公开回复或内部备注。
     *
     * @param args 工具参数
     * @param ctx 执行上下文
     * @return 消息 ID 及给模型转述的文本
     */
    @Override
    public ToolResult execute(Map<String, Object> args, ToolContext ctx) {
        Long ticketId = asLong(args, "ticketId");
        String content = str(args, "content");
        String visible = str(args, "visible");

        if (ticketId == null) {
            return ToolResult.fail("请提供要回复的工单 ID。");
        }
        if (content == null) {
            return ToolResult.fail("回复内容不能为空。");
        }
        if (!StatusConstant.VISIBLE_PUBLIC.equals(visible)
                && !StatusConstant.VISIBLE_INTERNAL.equals(visible)) {
            return ToolResult.fail("请指定回复类型：public（客户可见）或 internal（仅内部可见）。");
        }

        try {
            TicketReplyDTO dto = new TicketReplyDTO();
            dto.setContent(content);
            dto.setVisible(visible);

            Long messageId = messageService.reply(ticketId, dto, ctx.userId());

            // Service 层可能把提交人的 internal 降级成 public，返回话术要跟着说。
            boolean internal = StatusConstant.VISIBLE_INTERNAL.equals(visible);
            return ToolResult.ok(
                    Map.of("ticketId", ticketId,
                            "messageId", messageId,
                            "visible", visible),
                    internal
                            ? "已在工单 " + ticketId + " 下记了一条**内部备注**（客户看不到）。"
                            : "已回复工单 " + ticketId + "，客户可以看到这条内容。");
        } catch (BaseException e) {
            return ToolResult.fail(e.getMessage());
        } catch (Exception e) {
            log.error("工具 ticket_reply 执行失败", e);
            return ToolResult.fail("回复失败：" + e.getMessage());
        }
    }
}