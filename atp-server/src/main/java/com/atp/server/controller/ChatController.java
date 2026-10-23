package com.atp.server.controller;

import com.atp.agent.service.AgentChatService;
import com.atp.common.annotation.RequirePermission;
import com.atp.common.constant.MessageConstant;
import com.atp.common.exception.BaseException;
import com.atp.common.result.Result;
import com.atp.pojo.vo.kb.KbReferenceVO;
import com.atp.server.interceptor.JwtTokenInterceptor;
import com.atp.server.service.KbReferenceService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;

/**
 * AI 对话接口。
 *
 * 模型输出通过 Server-Sent Events 分段推送给前端
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class ChatController {

    // SSE 连接最长保留时间：5 分钟
    private static final long SSE_TIMEOUT_MS = 300_000L;

    // 对话编排服务
    private final AgentChatService chatService;

    // 知识库引用查询服务
    private final KbReferenceService referenceService;

    /**
     * 查询一次运行的知识库引用。
     *
     * @param runId 运行 ID
     * @return 引用列表
     */
    @GetMapping("/agents/runs/{runId}/references")
    @RequirePermission("kb:read")
    public Result<List<KbReferenceVO>> references(@PathVariable Long runId) {
        return Result.success(referenceService.listByRun(runId));
    }

    /**
     * AI 对话流式接口。
     *
     * 用户 ID 必须在返回 SseEmitter 之前同步读取。Flux 的订阅回调可能在其他线程执行，
     * 那时原请求已经结束，request attribute 不能再作为可靠的身份来源。
     *
     * @param memoryId 会话记忆 ID，格式为 userId:sessionId
     * @param message 用户消息
     * @param request 当前 HTTP 请求，用于同步读取 JWT 拦截器写入的调用者身份
     * @return SSE 连接
     * @throws BaseException 当前请求没有登录用户时抛出
     */
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestParam String memoryId,
                                 @RequestParam String message,
                                 HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtTokenInterceptor.ATTR_CALLER_ID);
        if (userId == null) {
            throw new BaseException(MessageConstant.NOT_LOGIN);
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        Flux<String> flux = chatService.chat(memoryId, message, userId);

        flux.subscribe(
                chunk -> {
                    try {
                        emitter.send(SseEmitter.event().data(chunk));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                },
                emitter::completeWithError,
                emitter::complete);

        emitter.onTimeout(emitter::complete);
        emitter.onError(error -> log.warn("SSE 连接异常：{}", error.getMessage()));
        return emitter;
    }
}
