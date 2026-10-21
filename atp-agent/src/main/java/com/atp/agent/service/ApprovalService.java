package com.atp.agent.service;

import com.atp.common.result.PageResult;
import com.atp.pojo.vo.agent.ApprovalVO;

import java.util.List;
import java.util.Map;

/**
 * 写操作审批服务。
 *
 */
public interface ApprovalService {

    /**
     * 为一次待审批写操作创建审批单。
     *
     * @param runId 运行 ID
     * @param memoryId 会话记忆 ID
     * @param stepSeq 工具步骤序号
     * @param toolCode 工具编码
     * @param args 工具参数
     * @param reason 审批原因
     * @return 审批单 ID；无法创建时返回 null
     */
    Long createPending(String runId,
                       String memoryId,
                       Integer stepSeq,
                       String toolCode,
                       Map<String, Object> args,
                       String reason);

    /**
     * 分页查询审批单。
     *
     * @param page 页码
     * @param pageSize 每页条数
     * @param status 状态筛选，可为空
     * @return 分页结果
     */
    PageResult<ApprovalVO> page(int page, int pageSize, String status);

    /**
     * 查询一次运行下的审批单。
     *
     * @param runId 运行 ID
     * @return 审批单列表
     */
    List<ApprovalVO> listByRun(Long runId);

    /**
     * 做审批决定。
     *
     * @param approvalId 审批单 ID
     * @param approved true 通过，false 驳回
     * @param operatorId 审批人 ID
     * @param comment 审批意见
     * @return 更新后的审批单
     */
    ApprovalVO decide(Long approvalId, boolean approved, Long operatorId, String comment);

    /**
     * 执行已通过的审批单。
     *
     * @param approvalId 审批单 ID
     * @return 执行后的审批单
     */
    ApprovalVO resume(Long approvalId);

    /**
     * 将超时的待审批单标记为 expired。
     *
     * @return 更新条数
     */
    int expireOutdated();
}
