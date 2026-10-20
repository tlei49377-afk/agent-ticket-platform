package com.atp.agent.tool.spi;

/**
 * 权限判断。实现放 atp-server，复用阶段 1 的 PermissionMapper + Redis 缓存。
 */
public interface PermissionChecker {

    /**
     * 判断用户是否拥有该权限码
     * @param userId 用户 ID
     * @param permissionCode 权限码
     * @return 有权限返回 true
     */
    boolean has(Long userId, String permissionCode);
}