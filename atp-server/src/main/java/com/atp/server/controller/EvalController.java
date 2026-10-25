package com.atp.server.controller;

import com.atp.common.annotation.RequirePermission;
import com.atp.common.result.Result;
import com.atp.eval.EvalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 固定评测集、跑批结果和回归对比接口。
 */
@RestController
@RequestMapping("/eval")
@RequiredArgsConstructor
public class EvalController {

    private final EvalService evalService;

    /**
     * 查询评测数据集。
     *
     * @return 数据集列表
     */
    @GetMapping("/datasets")
    @RequirePermission("eval:manage")
    public Result<List<Map<String, Object>>> datasets() {
        return Result.success(evalService.datasets());
    }

    /**
     * 查询数据集中的固定用例。
     *
     * @param datasetId 数据集 ID
     * @return 用例列表
     */
    @GetMapping("/cases")
    @RequirePermission("eval:manage")
    public Result<List<Map<String, Object>>> cases(@RequestParam Long datasetId) {
        return Result.success(evalService.cases(datasetId));
    }

    /**
     * 启动一次异步评测。
     *
     * @param datasetId 数据集 ID
     * @param agentId Agent ID
     * @param limit 可选执行条数，便于先做小批量验证
     * @return eval_run.id
     */
    @PostMapping("/run")
    @RequirePermission("eval:manage")
    public Result<Long> run(@RequestParam(defaultValue = "1") Long datasetId,
                            @RequestParam(defaultValue = "1") Long agentId,
                            @RequestParam(required = false) Integer limit) {
        return Result.success(evalService.start(datasetId, agentId, limit));
    }

    /**
     * 查询最近评测批次。
     *
     * @param limit 最多返回条数
     * @return 批次列表
     */
    @GetMapping("/runs")
    @RequirePermission("eval:manage")
    public Result<List<Map<String, Object>>> runs(@RequestParam(defaultValue = "20") int limit) {
        return Result.success(evalService.runs(limit));
    }

    /**
     * 查询一次评测批次。
     *
     * @param id 批次 ID
     * @return 批次详情
     */
    @GetMapping("/runs/{id}")
    @RequirePermission("eval:manage")
    public Result<Map<String, Object>> runDetail(@PathVariable Long id) {
        return Result.success(evalService.run(id));
    }

    /**
     * 查询逐条评测结果。
     *
     * @param id 批次 ID
     * @return 结果列表
     */
    @GetMapping("/runs/{id}/results")
    @RequirePermission("eval:manage")
    public Result<List<Map<String, Object>>> results(@PathVariable Long id) {
        return Result.success(evalService.results(id));
    }

    /**
     * 对比两次评测的回归指标。
     *
     * @param runA 基线批次
     * @param runB 新批次
     * @return 两侧指标和差值
     */
    @GetMapping("/compare")
    @RequirePermission("eval:manage")
    public Result<Map<String, Object>> compare(@RequestParam Long runA,
                                               @RequestParam Long runB) {
        return Result.success(evalService.compare(runA, runB));
    }
}
