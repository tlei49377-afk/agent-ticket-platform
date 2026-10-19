package com.atp.agent.mapper;

import com.atp.pojo.entity.agent.AgentMeta;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Agent 定义访问。阶段 3 只需要按编码取一条。
 */
@Mapper
public interface AgentMapper {

    /**
     * 按编码取一个启用中的 Agent
     * @param code 编码，如 aftersale
     * @return Agent 定义，取不到返回 null
     */
    AgentMeta getByCode(@Param("code") String code);
}
