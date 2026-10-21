package com.atp.agent.mapper;

import com.atp.pojo.entity.agent.AgentApproval;
import com.atp.pojo.vo.agent.ApprovalVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批单访问。
 */
@Mapper
public interface AgentApprovalMapper {

    /**
     * 新建审批单。
     *
     * @param approval 审批单
     * @return 影响行数
     */
    int insert(AgentApproval approval);

    /**
     * 按 ID 查询实体。
     *
     * @param id 审批单 ID
     * @return 审批单；不存在时返回 null
     */
    AgentApproval getById(@Param("id") Long id);

    /**
     * 按 ID 查询展示对象。
     *
     * @param id 审批单 ID
     * @return 审批单展示对象
     */
    ApprovalVO getVoById(@Param("id") Long id);

    /**
     * 分页查询审批单。
     *
     * @param status 状态筛选，可为空
     * @param offset 偏移量
     * @param limit 条数
     * @return 审批单列表
     */
    List<ApprovalVO> listPage(@Param("status") String status,
                              @Param("offset") int offset,
                              @Param("limit") int limit);

    /**
     * 统计审批单。
     *
     * @param status 状态筛选，可为空
     * @return 总条数
     */
    long count(@Param("status") String status);

    /**
     * 查询一次运行下的全部审批单。
     *
     * @param runId 运行 ID
     * @return 审批单列表
     */
    List<ApprovalVO> listByRun(@Param("runId") Long runId);

    /**
     * 查询同一 run / step / tool 的待审批单，用于防止重复创建。
     *
     * @param runId 运行 ID
     * @param stepSeq 步骤序号
     * @param toolCode 工具编码
     * @return 待审批单；不存在时返回 null
     */
    AgentApproval getPendingByRunStepTool(@Param("runId") Long runId,
                                          @Param("stepSeq") Integer stepSeq,
                                          @Param("toolCode") String toolCode);

    /**
     * 做审批决定，只有 pending 能更新。
     *
     * @param id 审批单 ID
     * @param status approved 或 rejected
     * @param operatorId 审批人
     * @param comment 审批意见
     * @param operatedAt 审批时间
     * @return 影响行数
     */
    int decide(@Param("id") Long id,
               @Param("status") String status,
               @Param("operatorId") Long operatorId,
               @Param("comment") String comment,
               @Param("operatedAt") LocalDateTime operatedAt);

    /**
     * 抢占执行权，只有 approved 能变成 executing。
     *
     * @param id 审批单 ID
     * @return 影响行数
     */
    int claimExecution(@Param("id") Long id);

    /**
     * 写执行终态，只有 executing 能更新。
     *
     * @param id 审批单 ID
     * @param status executed 或 execution_failed
     * @return 影响行数
     */
    int finishExecution(@Param("id") Long id, @Param("status") String status);

    /**
     * 把已过期的 pending 审批单置为 expired。
     *
     * @param now 当前时间
     * @return 影响行数
     */
    int expireOutdated(@Param("now") LocalDateTime now);
}
