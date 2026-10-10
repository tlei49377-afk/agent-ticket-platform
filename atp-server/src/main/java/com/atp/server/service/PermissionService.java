package com.atp.server.service;

import java.util.List;

public interface PermissionService {
    // 查某用户的权限码列表
    List<String> listCodesByUserId(Long userId);

    // 删除某用户的权限缓存
    void evictCache(Long userId);
}
