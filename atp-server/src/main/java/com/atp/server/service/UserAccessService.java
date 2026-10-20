package com.atp.server.service;

import com.atp.server.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 判断当前用户能看到多大范围的数据。数据隔离的规则只在这一处写。
 */
@Service
@RequiredArgsConstructor
public class UserAccessService {

    // 这三个角色算内部人员，能看到全部工单
    // 其他角色只能看到自己提交的工单
    private static final List<String> STAFF_ROLES = List.of("agent", "supervisor", "admin");

    private final SysUserMapper sysUserMapper;

    /**
     * 是不是内部人员：agent / supervisor / admin 之一
     * @param userId
     * @return
     */
    public boolean isInternalStaff(Long userId) {
        if (userId == null) {
            return false;
        }
        List<String> roles = sysUserMapper.listRoleCodesByUserId(userId);
        return roles.stream().anyMatch(STAFF_ROLES::contains);
    }

    /**
     * 返回必须强制施加的提交人过滤值：内部人员返回 null（不过滤），
     * 其余人返回自己的 userId（只能看到自己提的工单）。
     */
    public Long scopeRequesterId(Long userId) {
        return isInternalStaff(userId) ? null : userId;
    }
}
