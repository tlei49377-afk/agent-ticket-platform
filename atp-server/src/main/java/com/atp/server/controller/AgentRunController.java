package com.atp.server.controller;

import com.atp.agent.mapper.AgentRunMapper;
import com.atp.agent.mapper.AgentRunStepMapper;
import com.atp.agent.mapper.AgentSessionMapper;
import com.atp.agent.service.AgentSessionService;
import com.atp.common.exception.BaseException;
import com.atp.common.result.Result;
import com.atp.pojo.entity.agent.AgentRun;
import com.atp.pojo.entity.agent.AgentRunStep;
import com.atp.pojo.entity.agent.AgentSession;
import com.atp.server.interceptor.JwtTokenInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前端运行轨迹与知识库引用定位所需的只读接口。
 */
@RestController
@RequiredArgsConstructor
public class AgentRunController {

    private final AgentRunMapper runMapper;

    private final AgentRunStepMapper runStepMapper;

    private final AgentSessionMapper sessionMapper;

    private final AgentSessionService sessionService;

    private final HttpServletRequest request;

    /**
     * 查一个会话最近一次运行，流式回答结束后前端用它定位引用和审批。
     *
     * @param memoryId 会话记忆 ID
     * @return 最近一次运行；没有运行时返回 null
     */
    @GetMapping("/agents/sessions/latest-run")
    public Result<AgentRun> latestRun(@RequestParam String memoryId) {
        sessionService.requireOwned(callerId(), memoryId);
        AgentSession session = sessionMapper.getByMemoryId(memoryId);
        return Result.success(runMapper.getLatestBySessionId(session.getId()));
    }

    /**
     * 查一次运行的基本信息。
     *
     * @param runId 运行 ID
     * @return 运行记录
     */
    @GetMapping("/agents/runs/{runId}")
    public Result<AgentRun> getRun(@PathVariable Long runId) {
        return Result.success(requireOwnedRun(runId));
    }

    /**
     * 查一次运行的步骤轨迹。
     *
     * @param runId 运行 ID
     * @return 按 seq 升序的步骤
     */
    @GetMapping("/agents/runs/{runId}/steps")
    public Result<List<AgentRunStep>> steps(@PathVariable Long runId) {
        requireOwnedRun(runId);
        return Result.success(runStepMapper.listByRun(runId));
    }

    /**
     * 读取运行记录并校验它属于当前用户。
     *
     * @param runId 运行 ID
     * @return 运行记录
     */
    private AgentRun requireOwnedRun(Long runId) {
        AgentRun run = runMapper.getById(runId);
        if (run == null) {
            throw new BaseException("运行记录不存在");
        }
        AgentSession session = sessionMapper.getById(run.getSessionId());
        if (session == null) {
            throw new BaseException("运行所属会话不存在");
        }
        sessionService.requireOwned(callerId(), session.getMemoryId());
        return run;
    }

    /**
     * 从请求作用域读取当前用户 ID。
     *
     * @return 当前用户 ID
     */
    private Long callerId() {
        return (Long) request.getAttribute(JwtTokenInterceptor.ATTR_CALLER_ID);
    }
}
