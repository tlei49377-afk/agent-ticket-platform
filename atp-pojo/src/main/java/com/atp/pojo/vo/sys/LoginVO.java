package com.atp.pojo.vo.sys;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应结果。登录成功后返回给前端的用户信息与令牌。
 */
@Data
@AllArgsConstructor
public class LoginVO {

    //用户 ID
    private Long id;

    //用户名
    private String username;

    //姓名
    private String name;

    //登录令牌，后续请求放在请求头里带回来
    private String token;
}
