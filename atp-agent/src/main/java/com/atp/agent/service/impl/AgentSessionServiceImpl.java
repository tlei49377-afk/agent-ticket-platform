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

    private static final int MAX_SESSIONS = 10;

    private final AgentSessionMapper sessionMapper;
    private final AgentMessageMapper messageMapper;
    private final ChatMemoryStore chatMemoryStore;

    /**
     * 新建会话，memoryId 格式 {userId}:{sessionId}
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
        session.setTitle("新会话");
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
     * 当前用户的会话列表，最多 10 条
     * @param userId 用户 ID
     * @return 会话列表
     */
    @Override
    public List<SessionVO> listSessions(Long userId) {
        return sessionMapper.listByUser(userId, MAX_SESSIONS);
    }

    /**
     * 会话消息，先过归属校验
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
     * 删除会话：消息物理删、会话逻辑删、记忆清掉
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
     * 会话归属校验：不属于该用户直接抛异常
     * @param userId 当前登录人 ID
     * @param memoryId 记忆标识
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
     * 记一条消息，seq 取会话内当前最大值 + 1
     * @param memoryId 记忆标识
     * @param runId 所属 run，可空
     * @param role user/ai/tool/system
     * @param content 内容
     */
    @Override
    public void appendMessage(String memoryId, Long runId, String role, String content) {
        AgentSession session = sessionMapper.getByMemoryId(memoryId);
        if (session == null) {
            throw new BaseException("会话不存在");
        }
        AgentMessage message = new AgentMessage();
        message.setSessionId(session.getId());
        message.setRunId(runId);
        message.setRole(role);
        message.setContent(content);
        message.setTokenIn(0);
        message.setTokenOut(0);
        message.setCost(BigDecimal.ZERO);
        message.setLatencyMs(0);
        message.setSeq(messageMapper.nextSeq(session.getId()));
        messageMapper.insert(message);

        // 让会话列表按"最近聊过"排序
        sessionMapper.touchLastActive(memoryId);
    }
}