package com.atp.agent.service.impl;

import com.atp.agent.mapper.AgentRunMapper;
import com.atp.agent.mapper.AgentSessionMapper;
import com.atp.agent.service.AgentRunService;
import com.atp.common.constant.StatusConstant;
import com.atp.common.exception.BaseException;
import com.atp.common.exception.PermissionDeniedException;
import com.atp.pojo.entity.agent.AgentRun;
import com.atp.pojo.entity.agent.AgentSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Agent 运行记录实现。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AgentRunServiceImpl implements AgentRunService {

    /** run_no 的时间部分格式。 */
    private static final DateTimeFormatter RUN_NO_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /** error_msg 列宽上限。 */
    private static final int ERROR_MSG_MAX = 1000;

    private final AgentRunMapper runMapper;
    private final AgentSessionMapper sessionMapper;

    /**
     * 创建一次运行记录。
     *
     * @param memoryId 会话记忆 ID
     * @param userId 发起人 ID
     * @param message 用户输入
     * @return agent_run.id 的字符串形式
     * @throws BaseException 会话不存在时抛出
     * @throws PermissionDeniedException 会话不属于当前用户时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String startRun(String memoryId, Long userId, String message) {
        AgentSession session = sessionMapper.getByMemoryId(memoryId);
        if (session == null) {
            throw new BaseException("会话不存在");
        }
        if (!session.getUserId().equals(userId)) {
            throw new PermissionDeniedException("该会话不属于当前用户");
        }

        AgentRun run = new AgentRun();
        run.setRunNo(nextRunNo());
        run.setSessionId(session.getId());
        run.setAgentId(session.getAgentId());
        run.setUserId(userId);
        run.setTraceId(MDC.get("traceId"));
        run.setInputText(message);
        run.setStatus(StatusConstant.RUN_RUNNING);
        run.setPromptVersion("stage3-v1");
        runMapper.insert(run);

        if (run.getId() == null) {
            throw new BaseException("创建运行记录失败");
        }
        return String.valueOf(run.getId());
    }

    /**
     * 结束一次运行并写入终态。
     *
     * @param runId agent_run.id 的字符串形式
     * @param status 终态：success/failed/cancelled
     * @param outputText 最终输出，可空
     * @param latencyMs 总耗时毫秒
     * @param errorMsg 失败原因，成功时传 null
     */
    @Override
    public void finishRun(String runId,
                          String status,
                          String outputText,
                          long latencyMs,
                          String errorMsg) {
        Long id = parseRunId(runId);
        if (id == null) {
            log.warn("runId 非数字，跳过 agent_run 收尾：{}", runId);
            return;
        }
        int safeLatency = (int) Math.min(Integer.MAX_VALUE, Math.max(0L, latencyMs));
        runMapper.finish(id, status, outputText, safeLatency, truncate(errorMsg));
    }

    /**
     * 生成展示用 run 编号。
     *
     * @return 时间戳加随机数构成的唯一编号
     */
    private String nextRunNo() {
        int random = ThreadLocalRandom.current().nextInt(100_000, 1_000_000);
        return "RUN" + LocalDateTime.now().format(RUN_NO_TIME) + random;
    }

    /**
     * 把字符串 runId 解析成数据库主键。
     *
     * @param runId agent_run.id 的字符串形式
     * @return 数字主键；格式非法时返回 null
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

    /**
     * 截断错误信息，避免超过 agent_run.error_msg 列宽。
     *
     * @param errorMsg 原始错误信息
     * @return 截断后的错误信息
     */
    private String truncate(String errorMsg) {
        if (errorMsg == null || errorMsg.length() <= ERROR_MSG_MAX) {
            return errorMsg;
        }
        return errorMsg.substring(0, ERROR_MSG_MAX);
    }
}