package com.atp.server.agent.tool.impl;

import com.atp.agent.tool.AbstractAgentTool;
import com.atp.agent.tool.ToolContext;
import com.atp.agent.tool.ToolResult;
import com.atp.common.constant.StatusConstant;
import com.atp.pojo.entity.ticket.SlaTimer;
import com.atp.server.service.SlaTimerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 查 SLA 剩余时间。
 * 传工单 ID 查一个，不传就捞“即将超期”的。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SlaStatusQueryTool extends AbstractAgentTool {

    /**
     * 剩余时间少于这个数就算“即将超期”。
     */
    private static final long WARN_MINUTES = 60L;

    private final SlaTimerService slaTimerService;

    /**
     * 工具编码，必须与 agent_tool.code 一致。
     *
     * @return 工具编码
     */
    @Override
    public String code() {
        return "sla_status_query";
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
     * 查询指定工单或近期即将超期的 SLA 计时器。
     *
     * @param args 工具参数
     * @param ctx 执行上下文
     * @return SLA 数据及给模型转述的文本
     */
    @Override
    public ToolResult execute(Map<String, Object> args, ToolContext ctx) {
        Long ticketId = asLong(args, "ticketId");
        try {
            slaTimerService.refreshStates();

            List<SlaTimer> timers = ticketId != null
                    ? slaTimerService.listByTicket(ticketId)
                    : slaTimerService.listNotBreached();
            if (timers == null || timers.isEmpty()) {
                return ToolResult.ok(List.of(), ticketId == null
                        ? "当前没有即将超期的工单。"
                        : "该工单没有 SLA 计时器（可能没配策略）。");
            }

            List<Map<String, Object>> data = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            for (SlaTimer timer : timers) {
                long remain = remainMinutes(timer.getDeadline());
                if (ticketId == null && remain > WARN_MINUTES) {
                    continue;
                }

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("ticketId", timer.getTicketId());
                row.put("timerType", timer.getTimerType());
                row.put("state", timer.getState());
                row.put("deadline", timer.getDeadline());
                row.put("remainMinutes", remain);
                row.put("escalated", timer.getEscalated());
                data.add(row);

                sb.append("- 工单 ").append(timer.getTicketId())
                        .append(" ").append(StatusConstant.TIMER_RESPOND.equals(timer.getTimerType()) ? "响应" : "解决")
                        .append("：").append(remain < 0
                                ? "已超期 " + Math.abs(remain) + " 分钟"
                                : "还剩 " + remain + " 分钟")
                        .append(StatusConstant.SLA_BREACHED.equals(timer.getState()) ? "（已超期）" : "")
                        .append("\n");
            }
            if (data.isEmpty()) {
                return ToolResult.ok(List.of(), "最近 1 小时内没有要超期的工单。");
            }
            return ToolResult.ok(data, sb.toString());
        } catch (Exception e) {
            log.error("工具 sla_status_query 执行失败", e);
            return ToolResult.fail("查询 SLA 状态失败：" + e.getMessage());
        }
    }
}