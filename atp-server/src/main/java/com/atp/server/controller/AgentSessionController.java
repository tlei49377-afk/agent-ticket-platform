package com.atp.server.controller;

import com.atp.agent.service.AgentSessionService;
import com.atp.common.result.Result;
import com.atp.pojo.vo.agent.MessageVO;
import com.atp.pojo.vo.agent.SessionVO;
import com.atp.server.interceptor.JwtTokenInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前端对话工作台需要的会话 REST 接口。
 *
 * <p>流式对话仍由 {@link ChatController} 提供；这里只负责会话创建、列表、
 * 历史消息和删除。所有操作都以 JWT 中的用户身份做归属校验。</p>
 */
@RestController
@RequestMapping("/agents/sessions")
@RequiredArgsConstructor
public class AgentSessionController {

    private final AgentSessionService sessionService;

    private final HttpServletRequest request;

    /**
     * 新建会话。
     *
     * @param agentId Agent ID，默认售后助手
     * @return memoryId
     */
    @PostMapping
    public Result<String> create(@RequestParam(defaultValue = "1") Long agentId) {
        return Result.success(sessionService.createSession(agentId, callerId()));
    }

    /**
     * 当前用户最近活跃的会话。
     *
     * @return 会话列表
     */
    @GetMapping
    public Result<List<SessionVO>> list() {
        return Result.success(sessionService.listSessions(callerId()));
    }

    /**
     * 查询一个会话的历史消息。
     *
     * @param memoryId 会话记忆 ID
     * @return 消息列表
     */
    @GetMapping("/messages")
    public Result<List<MessageVO>> messages(@RequestParam String memoryId) {
        return Result.success(sessionService.listMessages(callerId(), memoryId));
    }

    /**
     * 删除一个会话及其消息和模型记忆。
     *
     * @param memoryId 会话记忆 ID
     * @return 成功结果
     */
    @DeleteMapping
    public Result<Void> delete(@RequestParam String memoryId) {
        sessionService.deleteSession(callerId(), memoryId);
        return Result.success();
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
