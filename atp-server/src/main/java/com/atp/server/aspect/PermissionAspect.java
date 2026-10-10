package com.atp.server.aspect;

import com.atp.common.annotation.RequirePermission;
import com.atp.common.constant.MessageConstant;
import com.atp.common.exception.PermissionDeniedException;
import com.atp.server.interceptor.JwtTokenInterceptor;
import com.atp.server.service.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 接口权限校验切面。
 */
@Aspect
@Component
@Slf4j
public class PermissionAspect {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private HttpServletRequest request;

    /**
     * 只切 controller 层。AI 工具的权限校验在 ToolExecutor 里做，
     * 不走切面 —— 因为工具执行不在请求线程上。
     */
    @Pointcut("execution(* com.atp.server.controller..*.*(..))")
    public void permissionPointCut() {
    }

    @Before("permissionPointCut()")
    public void checkPermission(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = AopUtils.getMostSpecificMethod(
                signature.getMethod(), joinPoint.getTarget().getClass());

        RequirePermission requirePermission = method.getAnnotation(RequirePermission.class);
        if (requirePermission == null) {
            return;
        }

        // 从 request 取身份，不从 ThreadLocal
        Object attr = request.getAttribute(JwtTokenInterceptor.ATTR_CALLER_ID);
        if (attr == null) {
            throw new PermissionDeniedException(MessageConstant.NOT_LOGIN);
        }
        Long userId = (Long) attr;

        if (!permissionService.listCodesByUserId(userId).contains(requirePermission.value())) {
            log.warn("用户 {} 缺少权限 {}", userId, requirePermission.value());
            throw new PermissionDeniedException(MessageConstant.NO_PERMISSION);
        }
    }
}
