package com.atp.server.agent.tool.spi;

import com.atp.agent.mapper.AgentRunStepMapper;
import com.atp.agent.tool.ToolContext;
import com.atp.agent.tool.spi.RunStepRecorder;
import com.atp.common.utils.JsonUtils;
import com.atp.pojo.entity.agent.AgentRunStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * RunStepRecorder 的 atp-server 实现，把一次工具执行落成一条 agent_run_step。
 *
 * <p>工具步骤固定挂在 {@code tool} 节点上；runId 取上下文里的数字串
 * （对应 agent_run.id），非数字时跳过落库。</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RunStepRecorderImpl implements RunStepRecorder {

    /** 工具执行所在的节点名，与状态图约定一致。 */
    private static final String NODE_TOOL = "tool";

    private final AgentRunStepMapper runStepMapper;

    /**
     * 记一条工具执行步骤。轨迹是旁路，落库失败只记日志，不打断工具主流程。
     *
     * @param ctx 执行上下文
     * @param toolCode 工具编码
     * @param status 步骤状态：ok/error/interrupted
     * @param detail 详情
     * @param costMs 耗时毫秒
     */
    @Override
    public void recordTool(ToolContext ctx, String toolCode, String status, String detail, long costMs) {
        Long runId = parseRunId(ctx.runId());
        if (runId == null) {
            log.warn("runId 非数字，跳过 agent_run_step 落库：runId={}, toolCode={}", ctx.runId(), toolCode);
            return;
        }
        try {
            AgentRunStep step = new AgentRunStep();
            step.setRunId(runId);
            step.setSeq(ctx.stepIndex() == null ? 0 : ctx.stepIndex());
            step.setNode(NODE_TOOL);
            step.setToolCode(toolCode);
            step.setStatus(status);
            step.setOutputSnap(JsonUtils.toJson(detail));
            step.setCostMs((int) costMs);
            runStepMapper.insert(step);
        } catch (Exception e) {
            log.error("工具 {} 运行步骤落库失败：runId={}", toolCode, runId, e);
        }
    }

    /**
     * 把上下文里的 runId 字符串解析成 agent_run.id。
     *
     * @param runId 上下文中的运行 ID
     * @return 数字 ID；非数字时返回 null
     */
    private Long parseRunId(String runId) {
        if (runId == null || runId.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(runId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}