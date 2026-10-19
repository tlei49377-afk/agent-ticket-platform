package com.atp.agent.mapper;

import com.atp.pojo.entity.agent.AgentToolMeta;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工具元数据访问。ToolExecutor 每执行一次工具就查一次，加了 Redis 缓存就不怕。
 */
@Mapper
public interface AgentToolMetaMapper {

    /**
     * 按编码取一个启用中的工具
     * @param code 工具编码，和 AgentTool.code() 必须一致
     * @return 工具元数据，取不到返回 null
     */
    AgentToolMeta getByCode(@Param("code") String code);

    /**
     * 全部启用中的工具
     * @return 工具元数据列表，按 id 排序
     */
    List<AgentToolMeta> listAll();
}
