package com.atp.agent.service.impl;

import com.atp.agent.mapper.AgentApprovalMapper;
import com.atp.agent.mapper.AgentRunMapper;
import com.atp.agent.mapper.AgentRunStepMapper;
import com.atp.agent.mapper.AgentSessionMapper;
import com.atp.agent.service.AgentSessionService;
import com.atp.agent.service.ApprovalService;
import com.atp.agent.tool.AgentToolScopeResolver;
import com.atp.agent.tool.ToolContext;
import com.atp.agent.tool.ToolExecutor;
import com.atp.agent.tool.ToolResult;
import com.atp.agent.tool.spi.UsernameResolver;
import com.atp.common.constant.StatusConstant;
import com.atp.common.exception.BaseException;
import com.atp.common.result.PageResult;
import com.atp.common.utils.JsonUtils;
import com.atp.pojo.entity.agent.AgentApproval;
import com.atp.pojo.entity.agent.AgentRun;
import com.atp.pojo.entity.agent.AgentSession;
import com.atp.pojo.vo.agent.ApprovalVO;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 写操作审批服务实现。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ApprovalServiceImpl implements ApprovalService {

    // 审批单有效期
    private static final int EXPIRE_MINUTES = 30;

    // 单页最大条数
    private static final int MAX_PAGE_SIZE = 200;

    // 审批参数反序列化类型
    private static final TypeReference<Map<String, Object>> ARGS_TYPE = new TypeReference<>() {
    };

    private final AgentApprovalMapper approvalMapper;
    private final AgentRunMapper runMapper;
    private final AgentRunStepMapper runStepMapper;
    private final AgentSessionMapper sessionMapper;
    private final AgentSessionService sessionService;
    private final ToolExecutor toolExecutor;
    private final AgentToolScopeResolver scopeResolver;
    private final UsernameResolver usernameResolver;

    /**
     * 为待审批工具调用创建持久化审批单。
     *
     * @param runId 运行 ID
     * @param memoryId 会话记忆 ID
     * @param stepSeq 工具步骤序号
     * @param toolCode 工具编码
     * @param args 工具参数
     * @param reason 审批原因
     * @return 审批单 ID；运行上下文缺失时返回 null
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPending(String runId,
                              String memoryId,
                              Integer stepSeq,
                              String toolCode,
                              Map<String, Object> args,
                              String reason) {
        Long numericRunId = parseLong(runId);
        if (numericRunId == null || stepSeq == null) {
            log.warn("缺少 runId/stepSeq，无法创建审批单：runId={}, step={}", runId, stepSeq);
            return null;
        }
        AgentSession session = sessionMapper.getByMemoryId(memoryId);
        if (session == null) {
            throw new BaseException("会话不存在，无法创建审批单");
        }

        AgentApproval existing = approvalMapper.getPendingByRunStepTool(
                numericRunId, stepSeq, toolCode);
        if (existing != null) {
            return existing.getId();
        }

        AgentApproval approval = new AgentApproval();
        approval.setRunId(numericRunId);
        approval.setSessionId(session.getId());
        approval.setStepSeq(stepSeq);
        approval.setToolCode(toolCode);
        approval.setArguments(JsonUtils.toJson(args));
        approval.setReason(reason);
        approval.setStatus(StatusConstant.APPROVAL_PENDING);
        approval.setExpireAt(LocalDateTime.now().plusMinutes(EXPIRE_MINUTES));
        approvalMapper.insert(approval);
        return approval.getId();
    }

    /**
     * 分页查询审批单。
     *
     * @param page 页码
     * @param pageSize 每页条数
     * @param status 状态筛选
     * @return 分页结果
     */
    @Override
    public PageResult<ApprovalVO> page(int page, int pageSize, String status) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, pageSize));
        int offset = (safePage - 1) * safeSize;
        List<ApprovalVO> records = approvalMapper.listPage(status, offset, safeSize);
        return new PageResult<>(approvalMapper.count(status), records);
    }

    /**
     * 查询一次运行下的审批单。
     *
     * @param runId 运行 ID
     * @return 审批单列表
     */
    @Override
    public List<ApprovalVO> listByRun(Long runId) {
        if (runId == null) {
            throw new BaseException("运行 ID 不能为空");
        }
        return approvalMapper.listByRun(runId);
    }

    /**
     * 做审批决定。通过后只进入 approved，真正执行由 resume 负责。
     *
     * @param approvalId 审批单 ID
     * @param approved true 通过，false 驳回
     * @param operatorId 审批人 ID
     * @param comment 审批意见
     * @return 更新后的审批单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApprovalVO decide(Long approvalId, boolean approved, Long operatorId, String comment) {
        AgentApproval approval = requireApproval(approvalId);
        ensurePendingAndNotExpired(approval);

        String targetStatus = approved
                ? StatusConstant.APPROVAL_APPROVED
                : StatusConstant.APPROVAL_REJECTED;
        int rows = approvalMapper.decide(
                approvalId, targetStatus, operatorId, comment, LocalDateTime.now());
        if (rows == 0) {
            throw new BaseException("该审批已被其他人处理");
        }

        if (!approved) {
            appendAiMessage(approval.getSessionId(), approval.getRunId(),
                    "审批已驳回，本次写操作没有执行。");
        }
        return approvalMapper.getVoById(approvalId);
    }

    /**
     * 执行已审批通过的写工具：
     * 使用 approved -> executing 的条件更新抢占执行权，保证同一张审批单
     * 只有一个请求能进入工具执行。
     *
     * @param approvalId 审批单 ID
     * @return 执行后的审批单
     */
    @Override
    public synchronized ApprovalVO resume(Long approvalId) {
        AgentApproval approval = requireApproval(approvalId);
        if (StatusConstant.APPROVAL_EXECUTED.equals(approval.getStatus())) {
            return approvalMapper.getVoById(approvalId);
        }
        if (!StatusConstant.APPROVAL_APPROVED.equals(approval.getStatus())) {
            throw new BaseException("审批单当前状态不能执行：" + approval.getStatus());
        }
        if (approvalMapper.claimExecution(approvalId) == 0) {
            AgentApproval latest = requireApproval(approvalId);
            if (StatusConstant.APPROVAL_EXECUTED.equals(latest.getStatus())) {
                return approvalMapper.getVoById(approvalId);
            }
            throw new BaseException("审批单正在执行或已被处理");
        }

        try {
            ToolResult result = executeApprovedTool(approval);
            String finalStatus = result.success()
                    ? StatusConstant.APPROVAL_EXECUTED
                    : StatusConstant.APPROVAL_EXECUTION_FAILED;
            approvalMapper.finishExecution(approvalId, finalStatus);
            appendAiMessage(approval.getSessionId(), approval.getRunId(),
                    result.message() == null ? "审批操作执行完成。" : result.message());
        } catch (Exception e) {
            log.error("审批单 {} 执行失败", approvalId, e);
            approvalMapper.finishExecution(
                    approvalId, StatusConstant.APPROVAL_EXECUTION_FAILED);
            appendAiMessage(approval.getSessionId(), approval.getRunId(),
                    "审批已通过，但操作执行失败：" + safeMessage(e));
        }
        return approvalMapper.getVoById(approvalId);
    }

    /**
     * 将超时未处理的审批单标记为 expired。
     *
     * @return 更新条数
     */
    @Override
    public int expireOutdated() {
        return approvalMapper.expireOutdated(LocalDateTime.now());
    }

    /**
     * 重建审批单原始 run 的执行上下文并调用已审批入口。
     *
     * @param approval 审批单
     * @return 工具执行结果
     */
    private ToolResult executeApprovedTool(AgentApproval approval) {
        AgentRun run = runMapper.getById(approval.getRunId());
        AgentSession session = sessionMapper.getById(approval.getSessionId());
        if (run == null || session == null) {
            return ToolResult.fail("运行记录或会话不存在，无法执行审批");
        }

        Map<String, Object> args = parseArgs(approval.getArguments());
        int step = runStepMapper.nextSeq(approval.getRunId());
        ToolContext ctx = new ToolContext(
                run.getUserId(),
                usernameResolver.getUsername(run.getUserId()),
                String.valueOf(run.getId()),
                step,
                run.getTraceId(),
                session.getMemoryId());

        Set<String> scope = scopeResolver.resolve(session.getMemoryId());
        return toolExecutor.executeApproved(
                String.valueOf(approval.getId()),
                approval.getToolCode(),
                args,
                ctx,
                scope);
    }

    /**
     * 读取并校验审批单存在。
     *
     * @param approvalId 审批单 ID
     * @return 审批单
     */
    private AgentApproval requireApproval(Long approvalId) {
        AgentApproval approval = approvalMapper.getById(approvalId);
        if (approval == null) {
            throw new BaseException("审批单不存在");
        }
        return approval;
    }

    /**
     * 校验审批单仍为 pending 且未超时。
     *
     * @param approval 审批单
     */
    private void ensurePendingAndNotExpired(AgentApproval approval) {
        if (!StatusConstant.APPROVAL_PENDING.equals(approval.getStatus())) {
            throw new BaseException("该审批已处理，当前状态：" + approval.getStatus());
        }
        if (approval.getExpireAt() != null
                && approval.getExpireAt().isBefore(LocalDateTime.now())) {
            // 不在这里更新并抛异常：同一事务抛异常会回滚状态更新。
            // 过期单由 ApprovalExpireTask 统一置为 expired。
            throw new BaseException("该审批已超时，请重新发起");
        }
    }

    /**
     * 反序列化工具参数。
     *
     * @param json 参数 JSON
     * @return 参数 Map；空内容返回空 Map
     */
    private Map<String, Object> parseArgs(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        Map<String, Object> args = JsonUtils.fromJson(json, ARGS_TYPE);
        return args == null ? Map.of() : args;
    }

    /**
     * 把审批执行结果写入会话历史。
     *
     * @param sessionId 会话 ID
     * @param runId 运行 ID
     * @param message 消息内容
     */
    private void appendAiMessage(Long sessionId, Long runId, String message) {
        AgentSession session = sessionMapper.getById(sessionId);
        if (session == null || session.getMemoryId() == null) {
            return;
        }
        try {
            sessionService.appendMessage(session.getMemoryId(), runId, "ai", message);
        } catch (Exception e) {
            log.warn("审批结果写入会话历史失败：runId={}, message={}", runId, e.getMessage());
        }
    }

    /**
     * 安全提取异常消息。
     *
     * @param error 异常
     * @return 可展示消息
     */
    private String safeMessage(Exception error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }

    /**
     * 把字符串 ID 解析为 Long。
     *
     * @param value 字符串
     * @return Long；非法时返回 null
     */
    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
