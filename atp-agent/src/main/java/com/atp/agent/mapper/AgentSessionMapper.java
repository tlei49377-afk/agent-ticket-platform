package com.atp.agent.mapper;

import com.atp.pojo.entity.agent.AgentSession;
import com.atp.pojo.vo.agent.SessionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Agent 会话访问。
 */
@Mapper
public interface AgentSessionMapper {

    /**
     * 新建会话，自增主键回填到入参的 id 上
     * @param session 会话，memoryId 由自增 id 拼出来，所以这里先留空
     * @return 影响行数
     */
    int insert(AgentSession session);

    /**
     * 回填 memoryId，格式 {userId}:{sessionId}，建完才能拼
     * @param id 会话 ID
     * @param memoryId 记忆标识
     * @return 影响行数
     */
    int updateMemoryId(@Param("id") Long id, @Param("memoryId") String memoryId);

    /**
     * 按 memoryId 取会话，校验归属用
     * @param memoryId 记忆标识
     * @return 会话，取不到返回 null
     */
    AgentSession getByMemoryId(@Param("memoryId") String memoryId);

    /**
     * 某个用户的会话列表，按最后活跃倒序
     * @param userId 用户 ID
     * @param limit 最多取几条
     * @return 会话列表
     */
    List<SessionVO> listByUser(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 刷新最后活跃时间
     * @param memoryId 记忆标识
     * @return 影响行数
     */
    int touchLastActive(@Param("memoryId") String memoryId);

    /**
     * 逻辑删除，带上 userId 一起判，防越权删别人的
     * @param memoryId 记忆标识
     * @param userId 当前登录人 ID
     * @return 影响行数
     */
    int deleteByMemoryId(@Param("memoryId") String memoryId, @Param("userId") Long userId);
}
