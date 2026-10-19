package com.atp.agent.mapper;

import com.atp.pojo.entity.agent.AgentRunStep;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 运行步骤访问。运行轨迹页就是查它。
 */
@Mapper
public interface AgentRunStepMapper {

    /**
     * 落一步，自增主键回填到入参的 id 上
     * @param step 步骤
     * @return 影响行数
     */
    int insert(AgentRunStep step);

    /**
     * 某个 run 的全部步骤，按 seq 升序
     * @param runId run ID
     * @return 步骤列表
     */
    List<AgentRunStep> listByRun(@Param("runId") Long runId);
}
