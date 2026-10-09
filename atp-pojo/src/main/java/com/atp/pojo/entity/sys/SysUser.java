package com.atp.pojo.entity.sys;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysUser {
    private Long id; // 主键
    private String username; // 登录名
    @JsonIgnore
    private String password; // 密码（BCrypt 哈希），永远不返回给前端
    private String name; // 姓名
    private String phone; // 手机号
    private String email; // 邮箱
    private String avatar; // 头像 URL
    private Integer status; // 状态：1 启用 0 禁用
    private LocalDateTime lastLoginAt; // 最后登录时间
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
    private Integer deleted; // 逻辑删除：0 未删 1 已删
}
