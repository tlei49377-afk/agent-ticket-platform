package com.atp.server.agent.tool;

import com.atp.agent.mapper.AgentApprovalMapper;
import com.atp.agent.mapper.AgentRunMapper;
import com.atp.agent.mapper.AgentRunStepMapper;
import com.atp.agent.mapper.AgentSessionMapper;
import com.atp.agent.service.AgentSessionService;
import com.atp.agent.service.impl.ApprovalServiceImpl;
import com.atp.agent.tool.AgentToolScopeResolver;
import com.atp.agent.tool.ToolContext;
import com.atp.agent.tool.ToolExecutor;
import com.atp.agent.tool.ToolResult;
import com.atp.agent.tool.spi.UsernameResolver;
import com.atp.common.constant.StatusConstant;
import com.atp.pojo.entity.agent.AgentApproval;
import com.atp.pojo.entity.agent.AgentRun;
import com.atp.pojo.entity.agent.AgentSession;
import com.atp.pojo.vo.agent.ApprovalVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 审批执行服务测试。
 */
class ApprovalServiceImplTest {

    /**
     * 审批通过后应重建上下文、调用已审批执行入口并写入会话历史。
     */
    @Test
    void resume_应执行已审批工具并写入结果() {
        AgentApprovalMapper approvalMapper = mock(AgentApprovalMapper.class);
        AgentRunMapper runMapper = mock(AgentRunMapper.class);
        AgentRunStepMapper runStepMapper = mock(AgentRunStepMapper.class);
        AgentSessionMapper sessionMapper = mock(AgentSessionMapper.class);
        AgentSessionService sessionService = mock(AgentSessionService.class);
        ToolExecutor toolExecutor = mock(ToolExecutor.class);
        AgentToolScopeResolver scopeResolver = mock(AgentToolScopeResolver.class);
        UsernameResolver usernameResolver = mock(UsernameResolver.class);

        ApprovalServiceImpl service = new ApprovalServiceImpl(
                approvalMapper, runMapper, runStepMapper, sessionMapper,
                sessionService, toolExecutor, scopeResolver, usernameResolver);

        AgentApproval approval = new AgentApproval();
        approval.setId(9L);
        approval.setRunId(11L);
        approval.setSessionId(22L);
        approval.setStepSeq(3);
        approval.setToolCode("ticket_assign");
        approval.setArguments("{\"ticketId\":1,\"assigneeId\":3}");
        approval.setStatus(StatusConstant.APPROVAL_APPROVED);

        AgentRun run = new AgentRun();
        run.setId(11L);
        run.setUserId(5L);
        run.setTraceId("trace-9");

        AgentSession session = new AgentSession();
        session.setId(22L);
        session.setMemoryId("5:22");

        ApprovalVO finished = new ApprovalVO();
        finished.setId(9L);
        finished.setStatus(StatusConstant.APPROVAL_EXECUTED);

        when(approvalMapper.getById(9L)).thenReturn(approval);
        when(approvalMapper.claimExecution(9L)).thenReturn(1);
        when(approvalMapper.getVoById(9L)).thenReturn(finished);
        when(runMapper.getById(11L)).thenReturn(run);
        when(sessionMapper.getById(22L)).thenReturn(session);
        when(runStepMapper.nextSeq(11L)).thenReturn(8);
        when(usernameResolver.getUsername(5L)).thenReturn("wangwu");
        when(scopeResolver.resolve("5:22")).thenReturn(Set.of("ticket_assign"));
        when(toolExecutor.executeApproved(
                eq("9"), eq("ticket_assign"), any(), any(), eq(Set.of("ticket_assign"))))
                .thenReturn(ToolResult.ok(Map.of("ticketId", 1L), "已完成转派"));

        ApprovalVO result = service.resume(9L);

        assertEquals(StatusConstant.APPROVAL_EXECUTED, result.getStatus());
        verify(approvalMapper).finishExecution(9L, StatusConstant.APPROVAL_EXECUTED);
        verify(sessionService).appendMessage("5:22", 11L, "ai", "已完成转派");

        ArgumentCaptor<ToolContext> ctx = ArgumentCaptor.forClass(ToolContext.class);
        verify(toolExecutor).executeApproved(
                eq("9"), eq("ticket_assign"), any(), ctx.capture(), eq(Set.of("ticket_assign")));
        assertEquals(5L, ctx.getValue().userId());
        assertEquals(8, ctx.getValue().stepIndex());
        assertEquals("trace-9", ctx.getValue().traceId());
    }
}
