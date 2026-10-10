package com.atp.server.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 给每个请求准备一个 traceId，放进 MDC 供日志使用，并写回响应头。
 *
 * 优先级设成最高：要保证它比其他过滤器先执行，日志里才都有 traceId。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";
    public static final String HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 上游带了就复用，保证整条链路是同一个 id
        String traceId = request.getHeader(HEADER);
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        MDC.put(TRACE_ID, traceId);
        response.setHeader(HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // MDC 是 ThreadLocal，不清理会在复用线程上串号
            MDC.remove(TRACE_ID);
        }
    }
}
