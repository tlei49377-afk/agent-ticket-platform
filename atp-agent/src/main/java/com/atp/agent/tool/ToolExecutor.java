package com.atp.agent.tool;

import com.atp.agent.mapper.AgentToolMetaMapper;
import com.atp.agent.tool.spi.AuditService;
import com.atp.agent.tool.spi.PermissionChecker;
import com.atp.agent.tool.spi.RunStepRecorder;
import com.atp.common.constant.MessageConstant;
import com.atp.common.utils.JsonUtils;
import com.atp.pojo.entity.agent.AgentToolMeta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 工具执行器：七道关。
 *
 * 顺序很重要，不能随意调换：
 *   1. 存在性
 *   2. Agent 授权
 *   3. 权限
 *   4. 幂等
 *   5. 审批
 *   6. 执行（超时 + 重试）
 *   7. 审计
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ToolExecutor {

    /** 幂等缓存 Redis 前缀。 */
    private static final String IDEMPOTENT_PREFIX = "tool:idem:";

    /** 工具注册表。 */
    private final AgentToolRegistry registry;

    /** 工具元数据 Mapper。 */
    private final AgentToolMetaMapper toolMetaMapper;

    /** 权限检查器。 */
    private final PermissionChecker permissionChecker;

    /** Redis 客户端，用于幂等缓存。 */
    private final StringRedisTemplate redis;

    /** 审计服务。 */
    private final AuditService auditService;

    /** 工具执行步骤记录器。 */
    private final RunStepRecorder stepRecorder;

    /**
     * 按七道关执行一次工具调用。
     *
     * @param toolCode 工具编码
     * @param args 工具参数
     * @param ctx 执行上下文
     * @param agentToolScope 当前 Agent 被授权的工具集合；null 表示不限制
     * @return 工具执行结果
     */
    public ToolResult execute(String toolCode,
                              Map<String, Object> args,
                              ToolContext ctx,
                              Set<String> agentToolScope) {

        long start = System.currentTimeMillis();

        // ---- 第 1 关：工具是否存在 ----
        AgentTool tool = registry.get(toolCode);
        if (tool == null) {
            log.warn("模型调用了不存在的工具：{}", toolCode);
            return recordStep(ctx, toolCode, "error",
                    ToolResult.fail(MessageConstant.TOOL_NOT_FOUND + "：" + toolCode), start);
        }

        // ---- 第 2 关：该 Agent 是否被授权 ----
        if (agentToolScope != null && !agentToolScope.contains(toolCode)) {
            log.warn("Agent 未被授权使用工具 {}", toolCode);
            return recordStep(ctx, toolCode, "error",
                    ToolResult.fail("当前助手不具备该能力"), start);
        }

        AgentToolMeta meta = toolMetaMapper.getByCode(toolCode);
        if (meta == null || !Integer.valueOf(1).equals(meta.getEnabled())) {
            return recordStep(ctx, toolCode, "error",
                    ToolResult.fail(MessageConstant.TOOL_NOT_FOUND), start);
        }

        // ---- 第 3 关：权限（返回自然语言，不抛异常） ----
        if (meta.getRequiredPermission() != null
                && !permissionChecker.has(ctx.userId(), meta.getRequiredPermission())) {
            log.warn("用户 {} 缺少权限 {}，工具 {} 被拒",
                    ctx.userId(), meta.getRequiredPermission(), toolCode);
            auditService.recordTool(ctx, toolCode, args, false, "权限不足",
                    System.currentTimeMillis() - start);
            return recordStep(ctx, toolCode, "error",
                    ToolResult.fail(MessageConstant.NO_PERMISSION), start);
        }

        // ---- 第 4 关：幂等 ----
        String idemKey = IDEMPOTENT_PREFIX + ctx.idempotentKey(toolCode, JsonUtils.toJson(args));
        if (Integer.valueOf(1).equals(meta.getIdempotent())) {
            String cached = redis.opsForValue().get(idemKey);
            if (cached != null) {
                log.info("幂等命中，直接返回上次结果：{}", idemKey);
                ToolResult cachedResult = JsonUtils.fromJson(cached, ToolResult.class);
                if (cachedResult != null) {
                    return recordStep(ctx, toolCode, "ok", cachedResult, start);
                }
            }
        }

        // ---- 第 5 关：审批（写操作不执行，直接返回待审批） ----
        if (Integer.valueOf(1).equals(meta.getNeedApproval())) {
            String reason = buildApprovalReason(toolCode, args);
            auditService.recordTool(ctx, toolCode, args, true, "待审批",
                    System.currentTimeMillis() - start);
            return recordStep(ctx, toolCode, "interrupted",
                    ToolResult.needApproval(reason, args), start);
        }

        // ---- 第 6 关：执行（超时 + 有条件的重试） ----
        ToolResult result = executeWithTimeoutAndRetry(tool, meta, args, ctx);

        // ---- 第 7 关：审计 + 缓存幂等结果 ----
        if (Integer.valueOf(1).equals(meta.getIdempotent()) && result.success()) {
            redis.opsForValue().set(idemKey, JsonUtils.toJson(result),
                    Duration.ofHours(24));
        }
        auditService.recordTool(ctx, toolCode, args, result.success(),
                result.message(), System.currentTimeMillis() - start);

        return recordStep(ctx, toolCode, result.success() ? "ok" : "error", result, start);
    }

    /**
     * 把工具执行结果写入运行步骤，并原样返回结果。
     *
     * <p>运行轨迹是旁路能力，记录失败不能覆盖或中断工具本身的结果。</p>
     *
     * @param ctx 执行上下文
     * @param toolCode 工具编码
     * @param status 步骤状态：ok/error/interrupted
     * @param result 工具结果
     * @param start 开始时间戳
     * @return 原工具结果
     */
    private ToolResult recordStep(ToolContext ctx,
                                  String toolCode,
                                  String status,
                                  ToolResult result,
                                  long start) {
        String detail = result.message();
        if (result.needApproval() && result.approvalReason() != null) {
            detail = result.approvalReason();
        }
        try {
            stepRecorder.recordTool(ctx, toolCode, status, detail,
                    Math.max(0L, System.currentTimeMillis() - start));
        } catch (Exception e) {
            log.warn("工具 {} 运行步骤记录失败，不影响执行结果：{}", toolCode, e.getMessage());
        }
        return result;
    }

    /**
     * 带超时和只读重试地执行工具。
     *
     * @param tool 工具实现
     * @param meta 工具元数据
     * @param args 工具参数
     * @param ctx 执行上下文
     * @return 工具执行结果；全部尝试失败时返回超时提示
     */
    private ToolResult executeWithTimeoutAndRetry(AgentTool tool,
                                                  AgentToolMeta meta,
                                                  Map<String, Object> args,
                                                  ToolContext ctx) {

        int maxRetry = Integer.valueOf(1).equals(meta.getReadOnly())
                ? Math.max(0, meta.getMaxRetry() == null ? 0 : meta.getMaxRetry()) : 0;
        long backoff = meta.getRetryBackoffMs() == null ? 0L : meta.getRetryBackoffMs();
        long timeoutMs = meta.getTimeoutMs() == null ? 3000L : meta.getTimeoutMs();

        for (int attempt = 0; attempt <= maxRetry; attempt++) {
            try {
                return CompletableFuture
                        .supplyAsync(() -> tool.execute(args, ctx))
                        .orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                        .join();
            } catch (Exception e) {
                log.warn("工具 {} 第 {} 次执行失败：{}", tool.code(), attempt + 1, e.getMessage());
                if (attempt < maxRetry) {
                    sleep(backoff * (1L << attempt)); // 指数退避
                }
            }
        }
        return ToolResult.fail(MessageConstant.TOOL_TIMEOUT);
    }

    /**
     * 构造待审批提示。
     *
     * @param toolCode 工具编码
     * @param args 工具参数
     * @return 审批原因
     */
    private String buildApprovalReason(String toolCode, Map<String, Object> args) {
        return "准备执行 " + toolCode + "，参数：" + JsonUtils.toJson(args);
    }

    /**
     * 线程休眠指定毫秒数。
     *
     * @param ms 休眠毫秒数
     */
    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}