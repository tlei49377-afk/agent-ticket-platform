package com.atp.agent.service;

import com.atp.agent.tool.AgentRunContextStore;
import com.atp.common.constant.StatusConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 对话编排服务：
 * 校验会话、创建 run、调用 Agent，并把消息与终态落库。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AgentChatService {

    // 估算 token 时每个 token 约对应的字符数。
    private static final int CHARS_PER_TOKEN = 4;

    private final TicketAgentService ticketAgentService;
    private final AgentSessionService sessionService;
    private final AgentRunService runService;
    private final AgentRunContextStore runContextStore;

    /**
     * 执行一轮流式对话。
     *
     * 方法返回 Flux 前会同步完成归属校验、创建 run、记录用户消息和设置工具上下文；
     * 流式内容随后可能在别的线程发射，所以这些依赖请求线程的操作不能推迟到订阅回调里。
     *
     * @param memoryId 会话记忆 ID
     * @param message 用户消息
     * @param userId 当前登录用户 ID
     * @return 模型输出的流式文本
     */
    public Flux<String> chat(String memoryId, String message, Long userId) {
        sessionService.requireOwned(userId, memoryId);
        sessionService.touch(memoryId, message);

        String runId = runService.startRun(memoryId, userId, message);
        Long runDbId = Long.valueOf(runId);

        try {
            sessionService.appendMessage(
                    memoryId,
                    runDbId,
                    "user",
                    message,
                    estimateTokens(message),
                    0,
                    BigDecimal.ZERO,
                    0);
        } catch (Exception e) {
            runService.finishRun(runId, StatusConstant.RUN_FAILED, null, 0L, e.getMessage());
            throw e;
        }

        runContextStore.begin(memoryId, runId, MDC.get("traceId"));

        long start = System.currentTimeMillis();
        StringBuilder output = new StringBuilder();
        AtomicBoolean finished = new AtomicBoolean(false);

        return Flux.defer(() -> ticketAgentService.chat(memoryId, message))
                .doOnNext(output::append)
                .doOnComplete(() -> {
                    if (finished.compareAndSet(false, true)) {
                        finishSuccess(memoryId, runId, output.toString(), start);
                    }
                })
                .doOnError(error -> {
                    if (finished.compareAndSet(false, true)) {
                        finishFailure(memoryId, runId, output.toString(), start, error);
                    }
                })
                .doFinally(signal -> {
                    if (signal == SignalType.CANCEL && finished.compareAndSet(false, true)) {
                        finishCancelled(memoryId, runId, output.toString(), start);
                    }
                    runContextStore.clear(memoryId, runId);
                });
    }

    /**
     * 成功结束时记录 AI 消息并收尾 run。
     *
     * @param memoryId 会话记忆 ID
     * @param runId run ID
     * @param output 完整模型输出
     * @param start 开始时间戳
     */
    private void finishSuccess(String memoryId, String runId, String output, long start) {
        long latency = System.currentTimeMillis() - start;
        try {
            sessionService.appendMessage(
                    memoryId,
                    Long.valueOf(runId),
                    "ai",
                    output,
                    0,
                    estimateTokens(output),
                    BigDecimal.ZERO,
                    (int) Math.min(Integer.MAX_VALUE, latency));
        } catch (Exception e) {
            log.error("AI 消息落库失败，runId={}", runId, e);
        } finally {
            runService.finishRun(runId, StatusConstant.RUN_SUCCESS, output, latency, null);
        }
    }

    /**
     * 失败时记录可读的 AI 失败消息并收尾 run。
     *
     * @param memoryId 会话记忆 ID
     * @param runId run ID
     * @param partialOutput 失败前已产生的部分输出
     * @param start 开始时间戳
     * @param error 失败原因
     */
    private void finishFailure(String memoryId, String runId, String partialOutput,
                               long start, Throwable error) {
        long latency = System.currentTimeMillis() - start;
        String message = "抱歉，本次回答生成失败，请稍后重试。";
        log.error("Agent 执行失败，runId={}", runId, error);
        try {
            sessionService.appendMessage(
                    memoryId,
                    Long.valueOf(runId),
                    "ai",
                    message,
                    0,
                    estimateTokens(message),
                    BigDecimal.ZERO,
                    (int) Math.min(Integer.MAX_VALUE, latency));
        } catch (Exception e) {
            log.error("失败消息落库失败，runId={}", runId, e);
        } finally {
            String errorMsg = error == null ? null : error.getClass().getSimpleName()
                    + ": " + error.getMessage();
            runService.finishRun(runId, StatusConstant.RUN_FAILED, partialOutput, latency, errorMsg);
        }
    }

    /**
     * 客户端取消流时收尾 run。
     *
     * @param memoryId 会话记忆 ID
     * @param runId run ID
     * @param partialOutput 取消前已产生的部分输出
     * @param start 开始时间戳
     */
    private void finishCancelled(String memoryId, String runId, String partialOutput, long start) {
        long latency = System.currentTimeMillis() - start;
        log.info("Agent 流被取消，runId={}, latencyMs={}", runId, latency);
        runService.finishRun(runId, "cancelled", partialOutput, latency, "客户端取消流式响应");
    }

    /**
     * 用“字符数 / 4”估算 token，拿不到 TokenUsage 时作为兜底值。
     *
     * @param text 待估算文本
     * @return 非负 token 估算值
     */
    private int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(1, text.length() / CHARS_PER_TOKEN);
    }
}