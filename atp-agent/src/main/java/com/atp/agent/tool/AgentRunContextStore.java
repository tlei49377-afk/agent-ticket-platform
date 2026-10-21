package com.atp.agent.tool;

import com.atp.common.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * 保存一次 Agent run 在工具线程中仍需要的上下文。
 *
 * <p>模型生成 tool_calls 后，工具不一定在原请求线程执行，MDC 也已经清理。
 * runId 与 traceId 因此按 memoryId 放入 Redis；工具步骤号由 Redis INCR 生成，
 * 保证同一 run 内多线程调用也不会重复。</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AgentRunContextStore {

    /** run 上下文的 Redis 前缀。 */
    private static final String CONTEXT_PREFIX = "atp:agent:run:context:";

    /** 步骤计数器的 Redis 前缀。 */
    private static final String STEP_PREFIX = "atp:agent:run:step:";

    /** 上下文与步骤计数器的保留时间，覆盖一次长对话及排障窗口。 */
    private static final Duration TTL = Duration.ofHours(6);

    private final StringRedisTemplate redis;

    /**
     * 开始一次 run，写入工具执行所需上下文。
     *
     * @param memoryId 会话记忆 ID
     * @param runId agent_run.id 的字符串形式
     * @param traceId 当前请求的链路 ID，可空
     */
    public void begin(String memoryId, String runId, String traceId) {
        RunContext context = new RunContext(runId, traceId);
        redis.opsForValue().set(contextKey(memoryId), JsonUtils.toJson(context), TTL);
        redis.delete(stepKey(runId));
    }

    /**
     * 读取当前会话正在执行的 run 上下文。
     *
     * @param memoryId 会话记忆 ID
     * @return run 上下文；不存在或内容损坏时返回空
     */
    public Optional<RunContext> current(String memoryId) {
        String json = redis.opsForValue().get(contextKey(memoryId));
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(JsonUtils.fromJson(json, RunContext.class));
        } catch (Exception e) {
            log.warn("读取 Agent run 上下文失败，memoryId={}", memoryId, e);
            return Optional.empty();
        }
    }

    /**
     * 为一次工具调用分配 run 内唯一步骤号。
     *
     * @param runId agent_run.id 的字符串形式
     * @return 从 1 开始递增的步骤号；runId 为空时返回 0
     */
    public int nextStep(String runId) {
        if (runId == null || runId.isBlank()) {
            return 0;
        }
        String key = stepKey(runId);
        Long value = redis.opsForValue().increment(key);
        redis.expire(key, TTL);
        return value == null ? 0 : value.intValue();
    }

    /**
     * 清理一次 run 的上下文和步骤计数器。
     *
     * @param memoryId 会话记忆 ID
     * @param runId agent_run.id 的字符串形式
     */
    public void clear(String memoryId, String runId) {
        redis.delete(contextKey(memoryId));
        if (runId != null && !runId.isBlank()) {
            redis.delete(stepKey(runId));
        }
    }

    /**
     * 会话上下文 Redis key。
     *
     * @param memoryId 会话记忆 ID
     * @return Redis key
     */
    private String contextKey(String memoryId) {
        return CONTEXT_PREFIX + memoryId;
    }

    /**
     * run 步骤计数器 Redis key。
     *
     * @param runId agent_run.id 的字符串形式
     * @return Redis key
     */
    private String stepKey(String runId) {
        return STEP_PREFIX + runId;
    }

    /**
     * 工具执行线程可读取的 run 上下文。
     *
     * @param runId agent_run.id 的字符串形式
     * @param traceId 链路追踪 ID
     */
    public record RunContext(String runId, String traceId) {
    }
}