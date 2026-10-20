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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 升级工单优先级。
 * 写操作，审批由 ToolExecutor 统一拦截。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TicketEscalateTool extends AbstractAgentTool {

    // 工单服务
    private final TicketService ticketService;

    /**
     * 工具编码，必须与 agent_tool.code 一致。
     *
     * @return 工具编码
     */
    @Override
    public String code() {
        return "ticket_escalate";
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
                "targetPriority", String.class,
                "reason", String.class
        );
    }

    /**
     * 提升工单优先级，显式阻止降级。
     *
     * @param args 工具参数
     * @param ctx 执行上下文
     * @return 优先级变更结果及给模型转述的文本
     */
    @Override
    public ToolResult execute(Map<String, Object> args, ToolContext ctx) {
        Long ticketId = asLong(args, "ticketId");
        String targetText = str(args, "targetPriority");
        String reason = str(args, "reason");

        if (ticketId == null) {
            return ToolResult.fail("请提供要升级的工单 ID。");
        }
        Integer target = parsePriority(targetText);
        if (target == null) {
            return ToolResult.fail("无法识别目标优先级「" + targetText
                    + "」，请用：紧急 / 高 / 中 / 低。");
        }
        if (reason == null) {
            return ToolResult.fail("升级优先级必须说明原因，请先问用户为什么升级。");
        }

        try {
            TicketDetailVO before = ticketService.detail(ticketId, ctx.userId());
            if (before.getPriority() != null && before.getPriority() < target) {
                // 数字越小越紧急，所以“目标数字更大”等于降级。
                return ToolResult.fail("工单当前优先级是「" + priorityName(before.getPriority())
                        + "」，比「" + priorityName(target)
                        + "」更高，不需要升级。要降级请用调整优先级接口。");
            }

            // 真实签名：changePriority(ticketId, targetPriority, reason, operatorId)。
            ticketService.changePriority(ticketId, target, reason, ctx.userId());

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("ticketId", ticketId);
            data.put("fromPriority", before.getPriority());
            data.put("toPriority", target);
            return ToolResult.ok(data,
                    "工单「" + before.getTitle() + "」的优先级已从「"
                            + priorityName(before.getPriority()) + "」升到「"
                            + priorityName(target) + "」。");
        } catch (BaseException e) {
            return ToolResult.fail(e.getMessage());
        } catch (Exception e) {
            log.error("工具 ticket_escalate 执行失败", e);
            return ToolResult.fail("升级失败：" + e.getMessage());
        }
    }
}
