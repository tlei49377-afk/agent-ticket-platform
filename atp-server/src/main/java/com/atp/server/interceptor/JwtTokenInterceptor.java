package com.atp.server.interceptor;

import com.atp.server.properties.JwtProperties;
import com.atp.server.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 校验拦截器。
 *
 * 解析出的身份放进 request attribute，而不是 ThreadLocal。
 */
@Component
@Slf4j
public class JwtTokenInterceptor implements HandlerInterceptor {

    public static final String ATTR_CALLER_ID = "callerId";
    public static final String ATTR_CALLER_NAME = "callerName";

    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String token = request.getHeader(jwtProperties.getAdminTokenName());

        try {
            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
            Long userId = Long.valueOf(claims.get("userId").toString());
            String username = String.valueOf(claims.get("username"));

            // 关键：放进 request 作用域，随请求一起走，不依赖线程
            request.setAttribute(ATTR_CALLER_ID, userId);
            request.setAttribute(ATTR_CALLER_NAME, username);

            MDC.put("userId", String.valueOf(userId));
            return true;

        } catch (Exception ex) {
            log.warn("token 校验失败：{}", ex.getMessage());
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // MDC 本身也是 ThreadLocal，用完必须清
        MDC.remove("userId");
    }
}

