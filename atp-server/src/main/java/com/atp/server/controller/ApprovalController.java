package com.atp.server.controller;

import com.atp.agent.service.ApprovalService;
import com.atp.common.annotation.RequirePermission;
import com.atp.common.result.PageResult;
import com.atp.common.result.Result;
import com.atp.pojo.dto.agent.ApprovalDecisionDTO;
import com.atp.pojo.vo.agent.ApprovalVO;
import com.atp.server.interceptor.JwtTokenInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 写操作审批接口。
 */
@RestController
@RequestMapping("/approvals")
@Slf4j
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    private final HttpServletRequest request;

    /**
     * 分页查询审批单。
     *
     * @param page 页码
     * @param pageSize 每页条数
     * @param status 状态筛选
     * @return 审批单分页结果
     */
    @GetMapping
    @RequirePermission("ticket:approve")
    public Result<PageResult<ApprovalVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status) {
        return Result.success(approvalService.page(page, pageSize, status));
    }

    /**
     * 查询一次运行下的审批单。
     *
     * @param runId 运行 ID
     * @return 审批单列表
     */
    @GetMapping("/by-run/{runId}")
    @RequirePermission("ticket:approve")
    public Result<List<ApprovalVO>> listByRun(@PathVariable Long runId) {
        return Result.success(approvalService.listByRun(runId));
    }

    /**
     * 审批通过或驳回。
     *
     * @param dto 审批决定
     * @return 更新后的审批单
     */
    @PostMapping("/decide")
    @RequirePermission("ticket:approve")
    public Result<ApprovalVO> decide(@Valid @RequestBody ApprovalDecisionDTO dto) {
        return Result.success(approvalService.decide(
                dto.getApprovalId(), Boolean.TRUE.equals(dto.getApproved()), callerId(), dto.getComment()));
    }

    /**
     * 执行已审批通过的写操作。
     *
     * @param id 审批单 ID
     * @return 执行后的审批单
     */
    @PostMapping("/{id}/resume")
    @RequirePermission("ticket:approve")
    public Result<ApprovalVO> resume(@PathVariable Long id) {
        return Result.success(approvalService.resume(id));
    }

    /**
     * 从请求作用域读取当前审批人 ID。
     *
     * @return 当前用户 ID
     */
    private Long callerId() {
        return (Long) request.getAttribute(JwtTokenInterceptor.ATTR_CALLER_ID);
    }
}
