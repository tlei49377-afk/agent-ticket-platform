package com.atp.server.agent.tool;

import com.atp.agent.mapper.AgentToolMetaMapper;
import com.atp.agent.tool.AgentTool;
import com.atp.agent.tool.AgentToolRegistry;
import com.atp.agent.tool.ToolContext;
import com.atp.agent.tool.ToolExecutor;
import com.atp.agent.tool.ToolResult;
import com.atp.agent.tool.spi.AuditService;
import com.atp.agent.tool.spi.PermissionChecker;
import com.atp.agent.tool.spi.RunStepRecorder;
import com.atp.pojo.entity.agent.AgentToolMeta;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ToolExecutor 七道关的单元测试。
 *
 * <p>用独立测试配置装配最小依赖，不启动真实 MySQL、Redis 和尚未实现的 SPI。</p>
 */
@SpringBootTest(classes = ToolExecutorTest.TestConfig.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class ToolExecutorTest {

    /**
     * 被测工具执行器。
     */
    private final ToolExecutor executor;

    /**
     * 工具注册表，测试中按场景替换具体工具。
     */
    private final AgentToolRegistry registry;

    /**
     * 工具元数据 Mapper。
     */
    private final AgentToolMetaMapper toolMetaMapper;

    /**
     * 权限检查器。
     */
    private final PermissionChecker permissionChecker;

    /**
     * StringRedisTemplate，测试中用内存 Map 模拟幂等缓存。
     */
    private final StringRedisTemplate redis;

    /**
     * Redis value 操作，测试中模拟 get/set。
     */
    private final ValueOperations<String, String> valueOperations;

    /**
     * 每个测试开始前准备 Redis 幂等缓存替身。
     */
    @BeforeEach
    void setUpRedis() {
        Map<String, String> cache = new HashMap<>();
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString()))
                .thenAnswer(invocation -> cache.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            cache.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(valueOperations).set(anyString(), anyString(), any(Duration.class));
    }

    /**
     * 同一个幂等键调用两次时，第二次应直接读取缓存，只执行一次工具。
     */
    @Test
    void 幂等_同一键调两次只执行一次() {
        AgentTool tool = mock(AgentTool.class);
        when(tool.code()).thenReturn("ticket_query");
        when(tool.execute(any(), any())).thenReturn(ToolResult.ok(Map.of("total", 1), "查询成功"));
        when(registry.get("ticket_query")).thenReturn(tool);
        when(toolMetaMapper.getByCode("ticket_query"))
                .thenReturn(meta("ticket_query", 1, 1, 0, null));

        ToolContext ctx = new ToolContext(1L, "admin", "run-test-1", 0, "trace1", "1:1");
        Map<String, Object> args = Map.of("status", "待受理");

        ToolResult first = executor.execute("ticket_query", args, ctx, Set.of("ticket_query"));
        ToolResult second = executor.execute("ticket_query", args, ctx, Set.of("ticket_query"));

        assertTrue(first.success());
        assertEquals(first.message(), second.message());
        verify(tool).execute(args, ctx);
    }

    /**
     * 写工具命中审批关后只能返回待审批，不能执行工具本体。
     */
    @Test
    void 写工具_必须返回待审批而不是直接执行() {
        AgentTool tool = mock(AgentTool.class);
        when(tool.code()).thenReturn("ticket_assign");
        when(registry.get("ticket_assign")).thenReturn(tool);
        when(toolMetaMapper.getByCode("ticket_assign"))
                .thenReturn(meta("ticket_assign", 0, 0, 1, "ticket:assign"));
        when(permissionChecker.has(1L, "ticket:assign")).thenReturn(true);

        ToolContext ctx = new ToolContext(1L, "admin", "run-test-2", 0, "trace2", "1:2");
        Map<String, Object> args = Map.of("ticketId", 1L, "assigneeId", 3L);
        ToolResult result = executor.execute("ticket_assign", args, ctx, Set.of("ticket_assign"));

        assertTrue(result.needApproval());
        verify(tool, never()).execute(any(), any());
    }

    /**
     * 已审批入口应跳过审批拦截，真实执行写工具，并以审批单 ID 做幂等。
     */
    @Test
    void 已审批_执行写工具且同一审批单只执行一次() {
        AgentTool tool = mock(AgentTool.class);
        when(tool.code()).thenReturn("ticket_assign");
        when(tool.execute(any(), any()))
                .thenReturn(ToolResult.ok(Map.of("ticketId", 1L), "转派成功"));
        when(registry.get("ticket_assign")).thenReturn(tool);
        when(toolMetaMapper.getByCode("ticket_assign"))
                .thenReturn(meta("ticket_assign", 0, 1, 1, "ticket:assign"));
        when(permissionChecker.has(1L, "ticket:assign")).thenReturn(true);

        ToolContext ctx = new ToolContext(1L, "admin", "run-test-4", 0, "trace4", "1:4");
        Map<String, Object> args = Map.of("ticketId", 1L, "assigneeId", 3L);

        ToolResult first = executor.executeApproved(
                "approval-1", "ticket_assign", args, ctx, Set.of("ticket_assign"));
        ToolResult second = executor.executeApproved(
                "approval-1", "ticket_assign", args, ctx, Set.of("ticket_assign"));

        assertTrue(first.success());
        assertFalse(first.needApproval());
        assertEquals(first.message(), second.message());
        verify(tool, times(1)).execute(args, ctx);
    }

    /**
     * 没有权限时应返回自然语言提示，而不是向调用方抛异常。
     */
    @Test
    void 无权限_返回自然语言而不是抛异常() {
        AgentTool tool = mock(AgentTool.class);
        when(tool.code()).thenReturn("ticket_assign");
        when(registry.get("ticket_assign")).thenReturn(tool);
        when(toolMetaMapper.getByCode("ticket_assign"))
                .thenReturn(meta("ticket_assign", 0, 0, 1, "ticket:assign"));
        when(permissionChecker.has(5L, "ticket:assign")).thenReturn(false);

        ToolContext ctx = new ToolContext(5L, "zhaoliu", "run-test-3", 0, "trace3", "5:1");
        ToolResult result = executor.execute("ticket_assign",
                Map.of("ticketId", 1L, "assigneeId", 3L), ctx, Set.of("ticket_assign"));

        assertFalse(result.success());
        assertEquals("你没有执行该操作的权限", result.message());
        verify(tool, never()).execute(any(), any());
    }

    /**
     * 构造一条测试用工具元数据。
     *
     * @param code 工具编码
     * @param readOnly 是否只读
     * @param idempotent 是否幂等
     * @param needApproval 是否需要审批
     * @param permission 所需权限码
     * @return 工具元数据
     */
    private AgentToolMeta meta(String code, int readOnly, int idempotent,
                               int needApproval, String permission) {
        AgentToolMeta meta = new AgentToolMeta();
        meta.setId(1L);
        meta.setCode(code);
        meta.setReadOnly(readOnly);
        meta.setIdempotent(idempotent);
        meta.setTimeoutMs(1000);
        meta.setMaxRetry(0);
        meta.setRetryBackoffMs(0);
        meta.setNeedApproval(needApproval);
        meta.setRequiredPermission(permission);
        meta.setEnabled(1);
        return meta;
    }

    /**
     * ToolExecutor 测试专用最小 Spring 配置。
     */
    @Configuration(proxyBeanMethods = false)
    static class TestConfig {

        /**
         * 提供工具注册表替身。
         *
         * @return mock 工具注册表
         */
        @Bean
        @Primary
        AgentToolRegistry agentToolRegistry() {
            return mock(AgentToolRegistry.class);
        }

        /**
         * 提供工具元数据 Mapper 替身。
         *
         * @return mock Mapper
         */
        @Bean
        @Primary
        AgentToolMetaMapper agentToolMetaMapper() {
            return mock(AgentToolMetaMapper.class);
        }

        /**
         * 提供权限检查器替身。
         *
         * @return mock 权限检查器
         */
        @Bean
        @Primary
        PermissionChecker permissionChecker() {
            return mock(PermissionChecker.class);
        }

        /**
         * 提供 Redis 客户端替身。
         *
         * @return mock Redis 客户端
         */
        @Bean
        @Primary
        StringRedisTemplate stringRedisTemplate() {
            return mock(StringRedisTemplate.class);
        }

        /**
         * 提供 Redis value 操作替身。
         *
         * @return mock value 操作
         */
        @Bean
        ValueOperations<String, String> valueOperations() {
            return mock(ValueOperations.class);
        }

        /**
         * 提供审计服务替身。
         *
         * @return mock 审计服务
         */
        @Bean
        @Primary
        AuditService auditService() {
            return mock(AuditService.class);
        }

        /**
         * 提供运行步骤记录器替身。
         *
         * @return mock 运行步骤记录器
         */
        @Bean
        @Primary
        RunStepRecorder runStepRecorder() {
            return mock(RunStepRecorder.class);
        }

        /**
         * 装配被测工具执行器。
         *
         * @param registry 工具注册表
         * @param toolMetaMapper 工具元数据 Mapper
         * @param permissionChecker 权限检查器
         * @param redis Redis 客户端
         * @param auditService 审计服务
         * @param stepRecorder 运行步骤记录器
         * @return 工具执行器
         */
        @Bean
        ToolExecutor toolExecutor(AgentToolRegistry registry,
                                  AgentToolMetaMapper toolMetaMapper,
                                  PermissionChecker permissionChecker,
                                  StringRedisTemplate redis,
                                  AuditService auditService,
                                  RunStepRecorder stepRecorder) {
            return new ToolExecutor(registry, toolMetaMapper, permissionChecker,
                    redis, auditService, stepRecorder);
        }
    }
}
