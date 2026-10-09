package com.atp.pojo.vo.sys;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应结果。登录成功后返回给前端的用户信息与令牌。
 */
@Data
@AllArgsConstructor
public class LoginVO {
    private Long id; // 用户 ID
    private String username; // 用户名
    private String name; // 姓名
    private String token; // 登录令牌
}
