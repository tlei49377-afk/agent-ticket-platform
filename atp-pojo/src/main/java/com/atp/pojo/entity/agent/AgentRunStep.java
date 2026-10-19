package com.atp.pojo.entity.agent;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 运行中的一步，对应 agent_run_step 表。节点和工具执行都往这里写一行。
 */
@Data
public class AgentRunStep {

    private Long id; // 主键
    private Long runId; // run ID
    private Integer seq; // 步骤序号
    private String node; // intake/retrieve/plan/tool/approval/respond/escalate
    private String inputSnap; // 输入快照，JSON 字符串
    private String outputSnap; // 输出快照，JSON 字符串
    private String toolCode; // 本步调用的工具，可空
    private String status; // ok/error/interrupted
    private String errorMsg; // 错误信息
    private Integer costMs; // 耗时毫秒
    private LocalDateTime createTime; // 创建时间
}
