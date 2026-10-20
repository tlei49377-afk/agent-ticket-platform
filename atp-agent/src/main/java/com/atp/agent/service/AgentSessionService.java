package com.atp.agent.service;

import com.atp.pojo.vo.agent.MessageVO;
import com.atp.pojo.vo.agent.SessionVO;

import java.util.List;

/**
 * 会话生命周期。memoryId 一律从接口传进来，不在 Service 里从 ThreadLocal 取。
 */
public interface AgentSessionService {

    /**
     * 新建会话，返回 memoryId（格式 {userId}:{sessionId}）
     * @param agentId Agent ID
     * @param userId 归属用户 ID
     * @return memoryId
     */
    String createSession(Long agentId, Long userId);

    /**
     * 当前用户的会话列表，最多 10 条
     * @param userId 用户 ID
     * @return 会话列表
     */
    List<SessionVO> listSessions(Long userId);

    /**
     * 会话消息
     * @param userId 当前登录人 ID
     * @param memoryId 记忆标识
     * @return 消息列表
     */
    List<MessageVO> listMessages(Long userId, String memoryId);

    /**
     * 删除会话（连同消息与记忆一起删）
     * @param userId 当前登录人 ID
     * @param memoryId 记忆标识
     */
    void deleteSession(Long userId, String memoryId);

    /**
     * 会话归属校验：不属于该用户直接抛异常
     * @param userId 当前登录人 ID
     * @param memoryId 记忆标识
     */
    void requireOwned(Long userId, String memoryId);

    /**
     * 记一条消息，seq 由服务自己算
     * @param memoryId 记忆标识
     * @param runId 所属 run，可空
     * @param role user/ai/tool/system
     * @param content 内容
     */
    void appendMessage(String memoryId, Long runId, String role, String content);
}