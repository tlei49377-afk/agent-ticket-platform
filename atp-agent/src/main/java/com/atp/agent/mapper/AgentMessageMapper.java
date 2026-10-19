package com.atp.agent.mapper;

import com.atp.pojo.entity.agent.AgentMessage;
import com.atp.pojo.vo.agent.MessageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Agent 消息访问。
 */
@Mapper
public interface AgentMessageMapper {

    /**
     * 落一条消息，自增主键回填到入参的 id 上
     * @param message 消息
     * @return 影响行数
     */
    int insert(AgentMessage message);

    /**
     * 会话内消息，按 seq 升序
     * @param memoryId 记忆标识
     * @return 消息列表
     */
    List<MessageVO> listByMemoryId(@Param("memoryId") String memoryId);

    /**
     * 会话内下一个 seq，取当前最大值加一
     * @param sessionId 会话 ID
     * @return 下一个序号，空会话返回 1
     */
    Integer nextSeq(@Param("sessionId") Long sessionId);

    /**
     * 物理删除整个会话的消息。会话本身是逻辑删除，消息没必要再留一份
     * @param memoryId 记忆标识
     * @return 影响行数
     */
    int deleteByMemoryId(@Param("memoryId") String memoryId);
}
