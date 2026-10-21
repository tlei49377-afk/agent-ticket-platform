package com.atp.server.agent.tool.spi;

import com.atp.agent.tool.spi.PermissionChecker;
import com.atp.server.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * PermissionChecker 的 atp-server 实现。
 *
 * 内部已经是 PermissionMapper 查询 + Redis 缓存，这里只做一次包含判断。
 */
@Component
@RequiredArgsConstructor
public class PermissionCheckerImpl implements PermissionChecker {

    private final PermissionService permissionService;

    /**
     * 判断用户是否拥有该权限码。
     *
     * @param userId 用户 ID
     * @param permissionCode 权限码
     * @return 有权限返回 true
     */
    @Override
    public boolean has(Long userId, String permissionCode) {
        if (userId == null || permissionCode == null) {
            return false;
        }
        return permissionService.listCodesByUserId(userId).contains(permissionCode);
    }
}