package com.atp.server.agent.tool.impl;

import com.atp.agent.tool.AbstractAgentTool;
import com.atp.agent.tool.ToolContext;
import com.atp.agent.tool.ToolResult;
import com.atp.pojo.vo.ticket.TicketStatsVO;
import com.atp.server.mapper.TicketMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

/**
 * 查统计指标。只读。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StatsQueryTool extends AbstractAgentTool {

    private final TicketMapper ticketMapper;

    /**
     * 工具编码，必须与 agent_tool.code 一致。
     *
     * @return 工具编码
     */
    @Override
    public String code() {
        return "stats_query";
    }

    /**
     * 声明工具参数类型。
     *
     * @return 参数名到参数类型的映射
     */
    @Override
    public Map<String, Class<?>> paramTypes() {
        return Map.of("dimension", String.class);
    }

    /**
     * 按日、周、月维度查询工单统计指标。
     *
     * @param args 工具参数
     * @param ctx 执行上下文
     * @return 统计数据及给模型转述的文本
     */
    @Override
    public ToolResult execute(Map<String, Object> args, ToolContext ctx) {
        String dimension = str(args, "dimension");
        if (dimension == null) {
            dimension = "日";
        }
        try {
            LocalDate today = LocalDate.now();
            LocalDateTime from = switch (dimension) {
                case "日", "day" -> today.atStartOfDay();
                case "周", "week" -> today.minusDays(6).atStartOfDay();
                case "月", "month" -> today.withDayOfMonth(1).atStartOfDay();
                default -> null;
            };
            if (from == null) {
                return ToolResult.fail("无法识别维度「" + dimension + "」，请用：日 / 周 / 月。");
            }

            TicketStatsVO stats = ticketMapper.stats(from);
            long total = stats.getTotal() == null ? 0 : stats.getTotal();
            long resolved = stats.getResolvedCount() == null ? 0 : stats.getResolvedCount();
            long breached = stats.getBreachedCount() == null ? 0 : stats.getBreachedCount();
            Double avgRespond = stats.getAvgRespondMinutes();
            long percent = total == 0 ? 0 : Math.round(resolved * 100.0 / total);

            String text = "本" + dimension + "共 " + total + " 张工单："
                    + "已解决/关闭 " + resolved + " 张（" + percent + "%），"
                    + "其中超期未解决 " + breached + " 张，"
                    + "平均首次响应 " + (avgRespond == null ? "-" : avgRespond + " 分钟") + "。"
                    + (LocalTime.now().getHour() < 12 ? "（今日数据尚未完整，仅供参考）" : "");

            return ToolResult.ok(stats, text);
        } catch (Exception e) {
            log.error("工具 stats_query 执行失败", e);
            return ToolResult.fail("查询统计失败：" + e.getMessage());
        }
    }
}