package com.atp.pojo.entity.agent;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Agent 定义，对应 agent 表。
 */
@Data
public class AgentMeta {

    private Long id; // 主键
    private String code; // 编码：presale / aftersale
    private String name; // 名称
    private String description; // 描述
    private String systemPrompt; // 系统提示词，为空则用 classpath 下的 system.txt
    private String modelName; // 模型名，如 qwen-plus
    private BigDecimal temperature; // 温度
    private Integer maxRounds; // 最大模型轮次
    private String toolScope; // 可用工具编码，JSON 数组字符串
    private String kbScope; // 可用知识库分类，JSON 数组字符串
    private Integer enabled; // 是否启用
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
    private Integer deleted; // 逻辑删除
}