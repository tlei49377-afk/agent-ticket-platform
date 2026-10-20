package com.atp.server.agent.tool.impl;

import com.atp.agent.tool.AbstractAgentTool;
import com.atp.agent.tool.ToolContext;
import com.atp.agent.tool.ToolResult;
import com.atp.common.constant.StatusConstant;
import com.atp.common.exception.BaseException;
import com.atp.pojo.vo.ticket.TicketDetailVO;
import com.atp.server.service.TicketService;
import com.atp.server.service.TicketStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 流转工单状态。
 * 写操作，且只能走状态机允许的白名单。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TicketUpdateStatusTool extends AbstractAgentTool {

    // 工单服务，用来查询详情和允许的下一状态。
    private final TicketService ticketService;

    /**
     * 工单状态流转服务。
     */
    private final TicketStatusService ticketStatusService;

    /**
     * 工具编码，必须与 agent_tool.code 一致。
     *
     * @return 工具编码
     */
    @Override
    public String code() {
        return "ticket_update_status";
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
                "targetStatus", String.class,
                "reason", String.class
        );
    }

    /**
     * 按状态机规则流转工单状态。
     *
     * @param args 工具参数
     * @param ctx 执行上下文
     * @return 状态流转结果及给模型转述的文本
     */
    @Override
    public ToolResult execute(Map<String, Object> args, ToolContext ctx) {
        Long ticketId = asLong(args, "ticketId");
        String targetStatusText = str(args, "targetStatus");
        String reason = str(args, "reason");

        if (ticketId == null) {
            return ToolResult.fail("请提供要流转的工单 ID。");
        }
        Integer targetStatus = parseStatus(targetStatusText);
        if (targetStatus == null) {
            return ToolResult.fail("无法识别目标状态「" + targetStatusText
                    + "」，请用：待受理/处理中/已挂起/待用户确认/已解决/已关闭。");
        }

        try {
            TicketDetailVO before = ticketService.detail(ticketId, ctx.userId());

            // 先告诉模型“能不能走”，别让它试错式地猜。
            if (!before.getAllowedNextStatus().contains(targetStatus)) {
                return ToolResult.fail("工单当前是「" + statusName(before.getStatus())
                        + "」，不能直接变成「" + statusName(targetStatus)
                        + "」。允许的目标是：" + before.getAllowedNextStatus() + "。");
            }
            if (targetStatus == StatusConstant.TICKET_CLOSED && reason == null) {
                return ToolResult.fail("关闭工单不可恢复，请先说明关闭原因。");
            }

            // 真实签名：transit(ticketId, targetStatus, operatorId, reason)。
            ticketStatusService.transit(ticketId, targetStatus, ctx.userId(), reason);

            return ToolResult.ok(
                    Map.of("ticketId", ticketId,
                            "fromStatus", before.getStatus(),
                            "toStatus", targetStatus),
                    "工单「" + before.getTitle() + "」已从「" + statusName(before.getStatus())
                            + "」流转到「" + statusName(targetStatus) + "」。");
        } catch (BaseException e) {
            return ToolResult.fail(e.getMessage());
        } catch (Exception e) {
            log.error("工具 ticket_update_status 执行失败", e);
            return ToolResult.fail("流转失败：" + e.getMessage());
        }
    }
}