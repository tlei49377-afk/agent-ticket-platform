package com.atp.agent.service.impl;

import com.atp.agent.mapper.AgentMessageMapper;
import com.atp.agent.mapper.AgentSessionMapper;
import com.atp.agent.service.AgentSessionService;
import com.atp.common.constant.MessageConstant;
import com.atp.common.exception.BaseException;
import com.atp.common.exception.PermissionDeniedException;
import com.atp.pojo.entity.agent.AgentMessage;
import com.atp.pojo.entity.agent.AgentSession;
import com.atp.pojo.vo.agent.MessageVO;
import com.atp.pojo.vo.agent.SessionVO;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话读写。
 * 归属校验只在这一个类里做，别的地方按 memoryId 操作前都要先过它。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AgentSessionServiceImpl implements AgentSessionService {

    /** 会话列表最多返回条数。 */
    private static final int MAX_SESSIONS = 10;

    /** 默认会话标题，首次用户消息到来时替换成摘要。 */
    private static final String DEFAULT_TITLE = "新会话";

    /** 会话标题最大长度，与 agent_session.title 的 200 字符列宽留出余量。 */
    private static final int TITLE_MAX_LENGTH = 30;

    private final AgentSessionMapper sessionMapper;
    private final AgentMessageMapper messageMapper;
    private final ChatMemoryStore chatMemoryStore;

    /**
     * 新建会话，memoryId 格式 {userId}:{sessionId}。
     *
     * @param agentId Agent ID
     * @param userId 归属用户 ID
     * @return memoryId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createSession(Long agentId, Long userId) {
        AgentSession session = new AgentSession();
        session.setAgentId(agentId);
        session.setUserId(userId);
        session.setTitle(DEFAULT_TITLE);
        session.setLastActiveAt(LocalDateTime.now());
        session.setStatus(1);
        // 先插，拿到自增 id 才能拼 memoryId
        sessionMapper.insert(session);

        String memoryId = userId + ":" + session.getId();
        session.setMemoryId(memoryId);
        sessionMapper.updateMemoryId(session.getId(), memoryId);

        return memoryId;
    }

    /**
     * 当前用户的会话列表，最多 10 条。
     *
     * @param userId 用户 ID
     * @return 会话列表
     */
    @Override
    public List<SessionVO> listSessions(Long userId) {
        return sessionMapper.listByUser(userId, MAX_SESSIONS);
    }

    /**
     * 会话消息，先过归属校验。
     *
     * @param userId 当前登录人 ID
     * @param memoryId 记忆标识
     * @return 消息列表
     */
    @Override
    public List<MessageVO> listMessages(Long userId, String memoryId) {
        requireOwned(userId, memoryId);
        return messageMapper.listByMemoryId(memoryId);
    }

    /**
     * 删除会话：消息物理删、会话逻辑删、记忆清掉。
     *
     * @param userId 当前登录人 ID
     * @param memoryId 记忆标识
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long userId, String memoryId) {
        requireOwned(userId, memoryId);
        messageMapper.deleteByMemoryId(memoryId);
        sessionMapper.deleteByMemoryId(memoryId, userId);
        chatMemoryStore.deleteMessages(memoryId);
    }

    /**
     * 会话归属校验：不属于该用户直接抛异常。
     *
     * @param userId 当前登录人 ID
     * @param memoryId 记忆标识
     * @throws BaseException 会话不存在时抛出
     * @throws PermissionDeniedException 会话不属于当前用户时抛出
     */
    @Override
    public void requireOwned(Long userId, String memoryId) {
        AgentSession session = sessionMapper.getByMemoryId(memoryId);
        if (session == null) {
            throw new BaseException("会话不存在");
        }
        if (!session.getUserId().equals(userId)) {
            log.warn("越权访问会话：用户 {} 试图访问 {}", userId, memoryId);
            throw new PermissionDeniedException(MessageConstant.SESSION_NOT_OWNED);
        }
    }

    /**
     * 刷新最后活跃时间，并在标题仍为默认值时写入首句摘要。
     *
     * @param memoryId 记忆标识
     * @param message 用户本轮输入
     */
    @Override
    public void touch(String memoryId, String message) {
        sessionMapper.touch(memoryId, summarize(message));
    }

    /**
     * 记一条基础消息，token、成本与耗时均记 0。
     *
     * @param memoryId 记忆标识
     * @param runId 所属 run，可空
     * @param role user/ai/tool/system
     * @param content 内容
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void appendMessage(String memoryId, Long runId, String role, String content) {
        appendMessage(memoryId, runId, role, content, 0, 0, BigDecimal.ZERO, 0);
    }

    /**
     * 记一条带用量信息的消息，seq 取会话内当前最大值 + 1。
     *
     * @param memoryId 记忆标识
     * @param runId 所属 run，可空
     * @param role user/ai/tool/system
     * @param content 内容
     * @param tokenIn 输入 token 数
     * @param tokenOut 输出 token 数
     * @param cost 本次调用成本
     * @param latencyMs 本次调用耗时毫秒
     * @throws BaseException 会话不存在时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void appendMessage(String memoryId,
                              Long runId,
                              String role,
                              String content,
                              int tokenIn,
                              int tokenOut,
                              BigDecimal cost,
                              int latencyMs) {
        AgentSession session = sessionMapper.getByMemoryId(memoryId);
        if (session == null) {
            throw new BaseException("会话不存在");
        }

        AgentMessage message = new AgentMessage();
        message.setSessionId(session.getId());
        message.setRunId(runId);
        message.setRole(role);
        message.setContent(content);
        message.setTokenIn(tokenIn);
        message.setTokenOut(tokenOut);
        message.setCost(cost == null ? BigDecimal.ZERO : cost);
        message.setLatencyMs(latencyMs);
        message.setSeq(messageMapper.nextSeq(session.getId()));
        messageMapper.insert(message);

        // 让会话列表按“最近聊过”排序
        sessionMapper.touchLastActive(memoryId);
    }

    /**
     * 把首条消息压缩成适合会话列表展示的短标题。
     *
     * @param message 用户输入
     * @return 最多 30 个字符的标题；空消息返回默认标题
     */
    private String summarize(String message) {
        if (message == null || message.isBlank()) {
            return DEFAULT_TITLE;
        }
        String normalized = message.replaceAll("\\s+", " ").trim();
        return normalized.length() <= TITLE_MAX_LENGTH
                ? normalized
                : normalized.substring(0, TITLE_MAX_LENGTH) + "…";
    }
}