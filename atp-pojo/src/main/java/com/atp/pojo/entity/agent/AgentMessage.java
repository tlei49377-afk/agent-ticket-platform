package com.atp.pojo.entity.agent;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Agent 消息，对应 agent_message 表。
 */
@Data
public class AgentMessage {

    private Long id; // 主键
    private Long sessionId; // 会话 ID
    private Long runId; // 所属 run，可空
    private String role; // user/ai/tool/system
    private String content; // 内容
    private String toolCalls; // 模型发起的工具调用，JSON 字符串
    private String toolCallId; // 工具结果对应的调用 ID
    private String toolCode; // 工具编码
    private Integer tokenIn; // 输入 token 数
    private Integer tokenOut; // 输出 token 数
    private BigDecimal cost; // 成本（元）
    private Integer latencyMs; // 耗时毫秒
    private Integer seq; // 会话内序号，保证会话内的顺序
    private LocalDateTime createTime; // 创建时间
}
