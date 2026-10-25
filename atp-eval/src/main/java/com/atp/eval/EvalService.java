package com.atp.eval;

import com.atp.agent.mapper.AgentApprovalMapper;
import com.atp.agent.mapper.AgentRunMapper;
import com.atp.agent.mapper.AgentRunStepMapper;
import com.atp.agent.service.AgentChatService;
import com.atp.agent.service.AgentSessionService;
import com.atp.common.exception.BaseException;
import com.atp.common.utils.JsonUtils;
import com.atp.pojo.entity.agent.AgentRun;
import com.atp.pojo.entity.agent.AgentRunStep;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 固定评测集执行与回归指标服务。
 *
 * <p>一批最多执行 24 条用例。每条用例创建独立会话，避免历史记忆互相污染。
 * 跑批在线程池中异步执行，接口先返回 evalRunId，前端/脚本轮询状态即可。</p>
 */
@Service
@Slf4j
public class EvalService {

    /** 评测调用使用的系统用户：管理员。 */
    private static final long EVAL_USER_ID = 1L;

    /** 单条用例的最大等待时间。 */
    private static final Duration CASE_TIMEOUT = Duration.ofSeconds(180);

    /** JSON 数组转换类型。 */
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final JdbcTemplate jdbc;
    private final AgentChatService chatService;
    private final AgentSessionService sessionService;
    private final AgentRunMapper runMapper;
    private final AgentRunStepMapper runStepMapper;
    private final AgentApprovalMapper approvalMapper;
    private final Executor evalExecutor;

    /**
     * 显式构造器用于给评测线程池加 {@code @Qualifier}，避免和其他 Executor 冲突。
     */
    public EvalService(JdbcTemplate jdbc,
                       AgentChatService chatService,
                       AgentSessionService sessionService,
                       AgentRunMapper runMapper,
                       AgentRunStepMapper runStepMapper,
                       AgentApprovalMapper approvalMapper,
                       @Qualifier("evalExecutor") Executor evalExecutor) {
        this.jdbc = jdbc;
        this.chatService = chatService;
        this.sessionService = sessionService;
        this.runMapper = runMapper;
        this.runStepMapper = runStepMapper;
        this.approvalMapper = approvalMapper;
        this.evalExecutor = evalExecutor;
    }

    /**
     * 启动一次异步评测。
     *
     * @param datasetId 数据集 ID
     * @param agentId Agent ID
     * @param limit 本次最多执行条数，null 表示全部
     * @return eval_run.id
     */
    public Long start(Long datasetId, Long agentId, Integer limit) {
        Integer caseCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM eval_case WHERE dataset_id = ?",
                Integer.class, datasetId);
        if (caseCount == null || caseCount == 0) {
            throw new BaseException("评测数据集没有用例");
        }

        int total = limit == null ? caseCount : Math.min(Math.max(limit, 1), caseCount);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO eval_run
                    (dataset_id, agent_id, model_name, prompt_version, status, total_cases, started_at)
                    VALUES (?, ?, 'qwen-plus', 'system-v1', 'running', ?, NOW())
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, datasetId);
            ps.setLong(2, agentId);
            ps.setInt(3, total);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new BaseException("创建评测批次失败");
        }
        Long evalRunId = key.longValue();
        evalExecutor.execute(() -> execute(evalRunId, datasetId, agentId, total));
        return evalRunId;
    }

    /**
     * 查询数据集。
     *
     * @return 数据集列表
     */
    public List<Map<String, Object>> datasets() {
        return jdbc.queryForList("""
                SELECT id, name, description, agent_id, case_count, version, create_time
                FROM eval_dataset WHERE deleted = 0 ORDER BY id
                """);
    }

    /**
     * 查询某个数据集的用例。
     *
     * @param datasetId 数据集 ID
     * @return 用例列表
     */
    public List<Map<String, Object>> cases(Long datasetId) {
        return jdbc.queryForList("""
                SELECT id, dataset_id, category, question, expected_tools, expected_keywords,
                       expected_doc_ids, expected_escalate, expected_approval, weight, remark
                FROM eval_case WHERE dataset_id = ? ORDER BY id
                """, datasetId);
    }

    /**
     * 查询最近的评测批次。
     *
     * @param limit 最多条数
     * @return 批次列表
     */
    public List<Map<String, Object>> runs(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return jdbc.queryForList("""
                SELECT id, dataset_id, agent_id, model_name, prompt_version, git_commit, status,
                       total_cases, passed_cases, pass_rate, tool_hit_rate, keyword_hit_rate,
                       doc_recall, approval_match_rate, no_tool_rate, avg_tool_calls,
                       avg_faithfulness, avg_relevance, avg_latency_ms, total_token,
                       started_at, ended_at
                FROM eval_run ORDER BY id DESC LIMIT ?
                """, safeLimit);
    }

    /**
     * 查询一次评测批次。
     *
     * @param id 批次 ID
     * @return 批次详情
     */
    public Map<String, Object> run(Long id) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT * FROM eval_run WHERE id = ?
                """, id);
        if (rows.isEmpty()) {
            throw new BaseException("评测批次不存在");
        }
        return rows.get(0);
    }

    /**
     * 查询一次评测批次的逐条结果。
     *
     * @param id 批次 ID
     * @return 结果列表
     */
    public List<Map<String, Object>> results(Long id) {
        run(id);
        return jdbc.queryForList("""
                SELECT r.*, c.category, c.question, c.expected_tools, c.expected_keywords,
                       c.expected_approval, c.weight, c.remark
                FROM eval_result r
                JOIN eval_case c ON c.id = r.case_id
                WHERE r.eval_run_id = ?
                ORDER BY r.case_id
                """, id);
    }

    /**
     * 对比两次评测的指标。
     *
     * @param runA 基线批次
     * @param runB 新批次
     * @return 两侧指标和差值
     */
    public Map<String, Object> compare(Long runA, Long runB) {
        Map<String, Object> a = run(runA);
        Map<String, Object> b = run(runB);
        String[] keys = {
                "pass_rate", "tool_hit_rate", "keyword_hit_rate", "approval_match_rate",
                "no_tool_rate", "avg_tool_calls", "avg_latency_ms"
        };
        Map<String, Object> diff = new LinkedHashMap<>();
        for (String key : keys) {
            BigDecimal av = decimal(a.get(key));
            BigDecimal bv = decimal(b.get(key));
            diff.put(key, bv.subtract(av));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("runA", a);
        result.put("runB", b);
        result.put("diff", diff);
        return result;
    }

    /**
     * 执行一批评测。整个批次失败时也会把 eval_run 标记为 failed。
     */
    private void execute(Long evalRunId, Long datasetId, Long agentId, int total) {
        try {
            List<CaseRow> cases = loadCases(datasetId, total);
            List<ResultRow> results = new ArrayList<>();
            for (CaseRow evaluationCase : cases) {
                results.add(runOne(evalRunId, agentId, evaluationCase));
            }
            EvalMetrics metrics = calculate(results);
            jdbc.update("""
                    UPDATE eval_run SET status = 'success', passed_cases = ?, pass_rate = ?,
                        tool_hit_rate = ?, keyword_hit_rate = ?, approval_match_rate = ?,
                        no_tool_rate = ?, avg_tool_calls = ?, avg_rounds = ?,
                        avg_latency_ms = ?, avg_cost = 0, total_token = 0, ended_at = NOW()
                    WHERE id = ?
                    """,
                    metrics.passed(), scale(metrics.passRate()), scale(metrics.toolHitRate()),
                    scale(metrics.keywordHitRate()), scale(metrics.approvalMatchRate()),
                    scale(metrics.noToolRate()), decimal2(metrics.avgToolCalls()),
                    decimal2(metrics.avgToolCalls()), metrics.avgLatencyMs(), evalRunId);
            log.info("评测完成：runId={}, passed={}/{}, toolHit={}, keywordHit={}, latency={}ms",
                    evalRunId, metrics.passed(), metrics.total(), metrics.toolHitRate(),
                    metrics.keywordHitRate(), metrics.avgLatencyMs());
        } catch (Exception e) {
            log.error("评测批次执行失败：runId={}", evalRunId, e);
            jdbc.update("UPDATE eval_run SET status = 'failed', ended_at = NOW() WHERE id = ?", evalRunId);
        }
    }

    /**
     * 执行单条用例，采集实际工具、答案、审批与耗时。
     */
    private ResultRow runOne(Long evalRunId, Long agentId, CaseRow c) {
        long begin = System.currentTimeMillis();
        String memoryId = null;
        String answer = "";
        String error = null;
        Set<String> actualTools = new LinkedHashSet<>();
        int approvalCount = 0;
        AgentRun run = null;

        try {
            memoryId = sessionService.createSession(agentId, EVAL_USER_ID);
            answer = chatService.chat(memoryId, c.question(), EVAL_USER_ID)
                    .collectList()
                    .block(CASE_TIMEOUT)
                    .stream()
                    .collect(Collectors.joining());
            long sessionId = Long.parseLong(memoryId.substring(memoryId.indexOf(':') + 1));
            run = runMapper.getLatestBySessionId(sessionId);
            if (run == null) {
                throw new IllegalStateException("评测运行记录不存在");
            }
            List<AgentRunStep> steps = runStepMapper.listByRun(run.getId());
            actualTools.addAll(steps.stream()
                    .map(AgentRunStep::getToolCode)
                    .filter(StringUtils::hasText)
                    .toList());
            approvalCount = approvalMapper.listByRun(run.getId()).size();
        } catch (Exception e) {
            error = e.getClass().getSimpleName() + ": " + e.getMessage();
            log.warn("评测用例失败：caseId={}, question={}", c.id(), c.question(), e);
        } finally {
            if (memoryId != null) {
                try {
                    sessionService.deleteSession(EVAL_USER_ID, memoryId);
                } catch (Exception e) {
                    log.debug("清理评测会话失败：{}", memoryId, e);
                }
            }
        }

        String finalAnswer = answer;
        boolean toolHit = c.expectedTools().isEmpty()
                ? actualTools.isEmpty()
                : actualTools.stream().anyMatch(c.expectedTools()::contains);
        boolean keywordHit = c.expectedKeywords().isEmpty()
                || c.expectedKeywords().stream().allMatch(k -> containsIgnoreCase(finalAnswer, k));
        boolean approvalMatch = (c.expectedApproval() == 1) == (approvalCount > 0);
        boolean passed = error == null && run != null
                && "success".equals(run.getStatus()) && toolHit && keywordHit && approvalMatch;
        int latency = run != null && run.getLatencyMs() != null
                ? run.getLatencyMs() : (int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - begin);

        ResultRow result = new ResultRow(
                c.id(), c.category(), c.question(), answer, JsonUtils.toJson(actualTools),
                toolHit, keywordHit, approvalCount, approvalMatch, passed, latency, error,
                run == null ? null : run.getTokenIn(), run == null ? null : run.getTokenOut());

        saveResult(evalRunId, result);
        if (run != null && approvalCount > 0) {
            jdbc.update("UPDATE agent_approval SET status = 'expired' WHERE run_id = ? AND status = 'pending'",
                    run.getId());
        }
        return result;
    }

    /**
     * 加载用例。
     */
    private List<CaseRow> loadCases(Long datasetId, int limit) {
        return jdbc.query("""
                SELECT id, category, question, expected_tools, expected_keywords, expected_approval
                FROM eval_case WHERE dataset_id = ? ORDER BY id LIMIT ?
                """, (rs, rowNum) -> new CaseRow(
                rs.getLong("id"), rs.getString("category"), rs.getString("question"),
                parseList(rs.getString("expected_tools")),
                parseList(rs.getString("expected_keywords")),
                rs.getInt("expected_approval")), datasetId, limit);
    }

    /**
     * 写入单条评测结果。
     */
    private void saveResult(Long evalRunId, ResultRow r) {
        jdbc.update("""
                INSERT INTO eval_result
                (eval_run_id, case_id, actual_answer, actual_tools, actual_doc_ids, escalate,
                 approval, tool_hit, keyword_hit, doc_hit, judge_score, judge_faithfulness,
                 judge_relevance, judge_reason, token_in, token_out, latency_ms, error_msg)
                VALUES (?, ?, ?, ?, JSON_ARRAY(), 0, ?, ?, ?, 0, NULL, NULL, NULL, NULL, ?, ?, ?, ?)
                """, evalRunId, r.caseId(), r.answer(), r.actualTools(),
                r.approvalCount() > 0 ? 1 : 0, r.toolHit() ? 1 : 0,
                r.keywordHit() ? 1 : 0, r.tokenIn() == null ? 0 : r.tokenIn(),
                r.tokenOut() == null ? 0 : r.tokenOut(), r.latencyMs(), r.error());
    }

    /**
     * 计算批次指标。
     */
    private EvalMetrics calculate(List<ResultRow> results) {
        int total = results.size();
        int passed = (int) results.stream().filter(ResultRow::passed).count();
        double toolHit = total == 0 ? 0 : results.stream().filter(ResultRow::toolHit).count() * 1.0 / total;
        List<ResultRow> keywordRows = results.stream()
                .filter(r -> !"edge".equals(r.category()))
                .toList();
        double keywordHit = keywordRows.isEmpty() ? 0 : keywordRows.stream()
                .filter(ResultRow::keywordHit).count() * 1.0 / keywordRows.size();
        double approvalMatch = total == 0 ? 0 : results.stream()
                .filter(ResultRow::approvalMatch).count() * 1.0 / total;
        List<ResultRow> edgeRows = results.stream().filter(r -> "edge".equals(r.category())).toList();
        double noTool = edgeRows.isEmpty() ? 0 : edgeRows.stream()
                .filter(r -> parseList(r.actualTools()).isEmpty()).count() * 1.0 / edgeRows.size();
        double avgTools = total == 0 ? 0 : results.stream()
                .mapToInt(r -> parseList(r.actualTools()).size()).average().orElse(0);
        int avgLatency = total == 0 ? 0 : (int) Math.round(results.stream()
                .mapToInt(ResultRow::latencyMs).average().orElse(0));
        return new EvalMetrics(total, passed, total == 0 ? 0 : passed * 1.0 / total,
                toolHit, keywordHit, approvalMatch, noTool, avgTools, avgLatency);
    }

    /**
     * 解析 JSON 字符串数组；失败时返回空列表。
     */
    private List<String> parseList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return JsonUtils.fromJson(json, STRING_LIST);
        } catch (Exception e) {
            return List.of();
        }
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        return source != null && keyword != null
                && source.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal scale(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal decimal2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 数据库中的评测用例。
     */
    private record CaseRow(Long id, String category, String question,
                           List<String> expectedTools, List<String> expectedKeywords,
                           int expectedApproval) {
    }

    /**
     * 单条结果，供落库和指标计算共用。
     */
    private record ResultRow(Long caseId, String category, String question, String answer,
                             String actualTools, boolean toolHit, boolean keywordHit,
                             int approvalCount, boolean approvalMatch, boolean passed,
                             int latencyMs, String error, Integer tokenIn, Integer tokenOut) {
    }

    /**
     * 批次汇总指标。
     */
    private record EvalMetrics(int total, int passed, double passRate, double toolHitRate,
                               double keywordHitRate, double approvalMatchRate, double noToolRate,
                               double avgToolCalls, int avgLatencyMs) {
    }
}


