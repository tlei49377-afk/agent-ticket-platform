package com.atp.pojo.vo.sys;

import lombok.Data;

import java.util.List;

/**
 * 当前登录人的完整身份信息。
 *
 * <p>登录接口只负责签发 token；前端拿到 token 后再查这里，
 * 用于恢复角色和权限，避免把权限码塞进 JWT 后导致角色变更不生效。</p>
 */
@Data
public class UserProfileVO {

    // 用户 ID
    private Long id;

    // 登录账号
    private String username;

    // 展示姓名
    private String name;

    // 角色编码列表
    private List<String> roles;

    // 权限码列表
    private List<String> permissions;
}
