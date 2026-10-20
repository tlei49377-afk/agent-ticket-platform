package com.atp.agent.tool;

import com.atp.common.constant.StatusConstant;

import java.util.Map;

/**
 * 工具基类。
 * 参数取值、码值翻中文这些小活都在这儿，子类只管业务。
 */
public abstract class AbstractAgentTool implements AgentTool {

    /**
     * 状态别名："待受理" / "pending" / "1" 都认，认不出来返回 null。
     */
    private static final Map<String, Integer> STATUS_ALIAS = Map.ofEntries(
            Map.entry("待受理", 1), Map.entry("pending", 1),
            Map.entry("处理中", 2), Map.entry("processing", 2),
            Map.entry("已挂起", 3), Map.entry("suspended", 3),
            Map.entry("待用户确认", 4), Map.entry("confirming", 4),
            Map.entry("已解决", 5), Map.entry("resolved", 5),
            Map.entry("已关闭", 6), Map.entry("closed", 6));

    /**
     * 优先级别名："紧急" / "urgent" / "1" 都认，认不出来返回 null。
     */
    private static final Map<String, Integer> PRIORITY_ALIAS = Map.ofEntries(
            Map.entry("紧急", 1), Map.entry("urgent", 1),
            Map.entry("高", 2), Map.entry("high", 2),
            Map.entry("中", 3), Map.entry("normal", 3),
            Map.entry("低", 4), Map.entry("low", 4));

    /**
     * 优先级码值到中文名的映射。
     */
    private static final Map<Integer, String> PRIORITY_NAME =
            Map.of(1, "紧急", 2, "高", 3, "中", 4, "低");

    /**
     * 取字符串参数，空串当没传。
     *
     * @param args 参数列表
     * @param key 参数名
     * @return 参数值；空串或不存在时返回 null
     */
    protected String str(Map<String, Object> args, String key) {
        Object v = args.get(key);
        if (v == null) {
            return null;
        }
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    /**
     * 取整数参数。模型偶尔把数字写成字符串，这里两种都接。
     *
     * @param args 参数列表
     * @param key 参数名
     * @return 参数值；无法解析时返回 null
     */
    protected Long asLong(Map<String, Object> args, String key) {
        Object v = args.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Number num) {
            return num.longValue();
        }
        try {
            return Long.valueOf(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 取布尔参数，支持 Boolean 和常见字符串写法。
     *
     * @param args 参数列表
     * @param key 参数名
     * @param defaultValue 参数缺失时的默认值
     * @return 布尔值；无法解析时按 Boolean.parseBoolean 处理
     */
    protected boolean asBool(Map<String, Object> args, String key, boolean defaultValue) {
        Object v = args.get(key);
        if (v == null) {
            return defaultValue;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(String.valueOf(v).trim());
    }

    /**
     * 把中文、英文或数字状态转成状态码。
     *
     * @param text 状态文本
     * @return 状态码；无法识别时返回 null
     */
    protected Integer parseStatus(String text) {
        if (text == null) {
            return null;
        }
        Integer byAlias = STATUS_ALIAS.get(text.trim());
        if (byAlias != null) {
            return byAlias;
        }
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 把中文、英文或数字优先级转成优先级码。
     *
     * @param text 优先级文本
     * @return 优先级码；无法识别时返回 null
     */
    protected Integer parsePriority(String text) {
        if (text == null) {
            return null;
        }
        Integer byAlias = PRIORITY_ALIAS.get(text.trim());
        if (byAlias != null) {
            return byAlias;
        }
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 返回状态码对应的中文名。
     *
     * @param status 状态码
     * @return 中文状态名；为空时返回“未知”
     */
    protected String statusName(Integer status) {
        return status == null ? "未知" : StatusConstant.ticketStatusName(status);
    }

    /**
     * 返回优先级码对应的中文名。
     *
     * @param priority 优先级码
     * @return 中文优先级名；为空或未知时返回“未知”
     */
    protected String priorityName(Integer priority) {
        return priority == null ? "未知" : PRIORITY_NAME.getOrDefault(priority, "未知");
    }

    /**
     * 计算距离截止时间还剩多少分钟，负数表示已超期。
     *
     * @param deadline 截止时间
     * @return 剩余分钟数；截止时间为空时返回 0
     */
    protected long remainMinutes(java.time.LocalDateTime deadline) {
        if (deadline == null) {
            return 0L;
        }
        return java.time.Duration.between(java.time.LocalDateTime.now(), deadline).toMinutes();
    }
}
