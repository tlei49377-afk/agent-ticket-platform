package com.atp.agent.mapper;

import com.atp.pojo.entity.agent.AgentRun;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Agent 运行记录访问。
 */
@Mapper
public interface AgentRunMapper {

    /**
     * 开一次运行，started_at 由 SQL 取 NOW()，不走入参
     * @param run 运行记录
     * @return 影响行数
     */
    int insert(AgentRun run);

    /**
     * 按展示编号取运行记录
     * @param runNo 运行编号
     * @return 运行记录，取不到返回 null
     */
    AgentRun getByRunNo(@Param("runNo") String runNo);

    /**
     * 按主键取运行记录。
     *
     * @param id 运行 ID
     * @return 运行记录；不存在时返回 null
     */
    AgentRun getById(@Param("id") Long id);

    /**
     * 收尾：写终态、输出、耗时、错误信息，ended_at 由 SQL 取 NOW()
     * @param id 运行记录 ID
     * @param status 终态：success/failed/cancelled
     * @param outputText 最终输出
     * @param latencyMs 总耗时毫秒
     * @param errorMsg 错误信息，成功时传 null
     * @return 影响行数
     */
    int finish(@Param("id") Long id,
               @Param("status") String status,
               @Param("outputText") String outputText,
               @Param("latencyMs") Integer latencyMs,
               @Param("errorMsg") String errorMsg);
}
