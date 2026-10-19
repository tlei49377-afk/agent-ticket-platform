package com.atp.pojo.entity.agent;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 一次 Agent 运行的记录，对应 agent_run 表。一次提问 = 一行。
 */
@Data
public class AgentRun {

    private Long id; // 主键
    private String runNo; // 展示用编号
    private Long sessionId; // 会话 ID
    private Long agentId; // Agent ID
    private Long userId; // 发起人
    private String traceId; // 链路 ID
    private String inputText; // 用户输入
    private String outputText; // 最终输出
    private String status; // running/waiting_approval/success/failed/cancelled
    private Integer nodeCount; // 经过的节点数
    private Integer toolCallCount; // 工具调用次数
    private Integer roundCount; // 模型轮次
    private Integer tokenIn; // 输入 token 数
    private Integer tokenOut; // 输出 token 数
    private BigDecimal cost; // 成本合计
    private Integer latencyMs; // 总耗时
    private String errorMsg; // 错误信息
    private String promptVersion; // 提示词版本
    private String gitCommit; // 代码版本
    private LocalDateTime startedAt; // 开始时间
    private LocalDateTime endedAt; // 结束时间
}
