package com.atp.server.agent.tool.impl;

import com.atp.agent.tool.AbstractAgentTool;
import com.atp.agent.tool.ToolContext;
import com.atp.agent.tool.ToolResult;
import com.atp.common.exception.BaseException;
import com.atp.pojo.dto.ticket.TicketCreateDTO;
import com.atp.server.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 创建工单。
 * 写操作，审批由 ToolExecutor 统一拦截。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TicketCreateTool extends AbstractAgentTool {


    // 工单服务
    private final TicketService ticketService;

    /**
     * 工具编码，必须与 agent_tool.code 一致。
     *
     * @return 工具编码
     */
    @Override
    public String code() {
        return "ticket_create";
    }

    /**
     * 声明工具参数类型。
     *
     * @return 参数名到参数类型的映射
     */
    @Override
    public Map<String, Class<?>> paramTypes() {
        return Map.of(
                "title", String.class,
                "content", String.class,
                "categoryId", Long.class,
                "priority", String.class,
                "requesterId", Long.class
        );
    }

    /**
     * 为新问题创建一张 AI 渠道工单。
     *
     * @param args 工具参数
     * @param ctx 执行上下文
     * @return 新工单 ID 及给模型转述的文本
     */
    @Override
    public ToolResult execute(Map<String, Object> args, ToolContext ctx) {
        String title = str(args, "title");
        String content = str(args, "content");
        Long categoryId = asLong(args, "categoryId");
        String priority = str(args, "priority");
        Long requesterId = asLong(args, "requesterId");

        if (title == null) {
            return ToolResult.fail("请先给出工单标题。");
        }
        if (content == null) {
            return ToolResult.fail("请把用户描述的问题整理成一段话再建单。");
        }
        Integer priorityValue = parsePriority(priority);
        if (priority == null || priorityValue == null) {
            return ToolResult.fail("请先确认优先级：紧急 / 高 / 中 / 低。");
        }
        // 本版本只能给自己建单。要“帮别人建单”得给 TicketService.create 加一个提交人参数。
        if (requesterId != null && !requesterId.equals(ctx.userId())) {
            return ToolResult.fail("当前版本只支持为自己建单，请让用户自己提交。");
        }

        try {
            TicketCreateDTO dto = new TicketCreateDTO();
            dto.setTitle(title);
            dto.setContent(content);
            dto.setCategoryId(categoryId);
            dto.setPriority(priorityValue);
            // 渠道写死为 ai，便于后续统计 AI 建单占比。
            dto.setChannel("ai");

            Long ticketId = ticketService.create(dto, ctx.userId());

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("ticketId", ticketId);
            data.put("title", title);
            data.put("priority", priorityValue);
            return ToolResult.ok(data,
                    "已创建工单「" + title + "」，编号 ID " + ticketId
                            + "，优先级 " + priorityName(priorityValue) + "。");
        } catch (BaseException e) {
            return ToolResult.fail(e.getMessage());
        } catch (Exception e) {
            log.error("工具 ticket_create 执行失败", e);
            return ToolResult.fail("创建工单失败：" + e.getMessage());
        }
    }
}