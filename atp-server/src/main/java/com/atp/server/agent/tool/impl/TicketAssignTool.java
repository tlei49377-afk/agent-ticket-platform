package com.atp.server.agent.tool.impl;

import com.atp.agent.tool.AbstractAgentTool;
import com.atp.agent.tool.ToolContext;
import com.atp.agent.tool.ToolResult;
import com.atp.common.exception.BaseException;
import com.atp.pojo.vo.ticket.TicketDetailVO;
import com.atp.server.mapper.SysUserMapper;
import com.atp.server.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 指派或转派工单。
 * 写操作，审批由 ToolExecutor 统一拦截。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TicketAssignTool extends AbstractAgentTool {


    // 工单服务
    private final TicketService ticketService;

    /**
     * 系统用户 Mapper，用来校验被指派人和回显姓名。
     */
    private final SysUserMapper sysUserMapper;

    /**
     * 工具编码，必须与 agent_tool.code 一致。
     *
     * @return 工具编码
     */
    @Override
    public String code() {
        return "ticket_assign";
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
                "assigneeId", Long.class,
                "reason", String.class
        );
    }

    /**
     * 将工单指派或转派给指定用户。
     *
     * @param args 工具参数
     * @param ctx 执行上下文
     * @return 指派结果及给模型转述的文本
     */
    @Override
    public ToolResult execute(Map<String, Object> args, ToolContext ctx) {
        Long ticketId = asLong(args, "ticketId");
        Long assigneeId = asLong(args, "assigneeId");
        String reason = str(args, "reason");

        // 参数校验要严，缺参数直接返回提示，不要猜。
        if (ticketId == null) {
            return ToolResult.fail("请提供要指派的工单 ID 或工单号。");
        }
        if (assigneeId == null) {
            return ToolResult.fail("请提供要指派给谁。如果不确定 userId，先用 user_query 查一下。");
        }

        try {
            String name = sysUserMapper.getNameById(assigneeId);
            if (name == null) {
                return ToolResult.fail("没有找到该用户，请确认姓名是否正确。");
            }
            TicketDetailVO ticket = ticketService.detail(ticketId, ctx.userId());

            // 注意参数顺序：reason 在 operatorId 前面。
            ticketService.assign(ticketId, assigneeId, reason, ctx.userId());

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("ticketId", ticketId);
            data.put("assigneeId", assigneeId);
            data.put("assigneeName", name);
            return ToolResult.ok(data,
                    "已把工单「" + ticket.getTitle() + "」转派给 " + name + "。");
        } catch (BaseException e) {
            return ToolResult.fail(e.getMessage());
        } catch (Exception e) {
            log.error("工具 ticket_assign 执行失败", e);
            return ToolResult.fail("转派失败：" + e.getMessage());
        }
    }
}