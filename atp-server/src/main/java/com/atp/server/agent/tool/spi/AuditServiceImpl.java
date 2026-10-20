package com.atp.server.agent.tool.spi;

import com.atp.agent.tool.ToolContext;
import com.atp.agent.tool.spi.AuditService;
import com.atp.common.utils.JsonUtils;
import com.atp.pojo.entity.sys.SysAuditLog;
import com.atp.server.mapper.SysAuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AuditService 的 atp-server 实现，把一次工具调用落到 sys_audit_log。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    /** 工具执行的动作码，与建表注释一致。 */
    private static final String ACTION_TOOL_EXEC = "TOOL_EXEC";

    /** error_msg 列长度上限，超长截断避免整条审计写不进去。 */
    private static final int ERROR_MSG_MAX = 500;

    private final SysAuditLogMapper auditLogMapper;

    /**
     * 记一条工具调用审计。审计是旁路，落库失败只记日志，不打断工具主流程。
     *
     * @param ctx 执行上下文
     * @param toolCode 工具编码
     * @param args 调用参数
     * @param success 是否成功
     * @param detail 详情，通常是失败原因或"待审批"
     * @param costMs 耗时毫秒
     */
    @Override
    public void recordTool(ToolContext ctx, String toolCode, Map<String, Object> args,
                           boolean success, String detail, long costMs) {
        try {
            SysAuditLog log = new SysAuditLog();
            log.setUserId(ctx.userId());
            log.setUsername(ctx.username());
            log.setAction(ACTION_TOOL_EXEC);
            log.setToolCode(toolCode);
            log.setSuccess(success ? 1 : 0);
            log.setCostMs((int) costMs);
            log.setTraceId(ctx.traceId());
            log.setDetail(buildDetail(args, detail));
            if (!success) {
                log.setErrorMsg(truncate(detail, ERROR_MSG_MAX));
            }
            auditLogMapper.insert(log);
        } catch (Exception e) {
            log.error("工具 {} 审计落库失败", toolCode, e);
        }
    }

    /**
     * 把调用参数与结果摘要合成 detail 列所需的 JSON。
     *
     * @param args 调用参数
     * @param note 结果摘要（成功文案 / 失败原因 / "待审批"）
     * @return JSON 字符串
     */
    private String buildDetail(Map<String, Object> args, String note) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("args", args);
        detail.put("note", note);
        return JsonUtils.toJson(detail);
    }

    /**
     * 截断超长文本。
     *
     * @param text 原文本
     * @param max 最大长度
     * @return 截断后的文本
     */
    private String truncate(String text, int max) {
        if (text == null || text.length() <= max) {
            return text;
        }
        return text.substring(0, max);
    }
}