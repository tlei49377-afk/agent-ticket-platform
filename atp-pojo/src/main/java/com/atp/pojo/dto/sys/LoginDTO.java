package com.atp.pojo.dto.sys;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录入参。对应前端 POST /login。
 */
@Data
public class LoginDTO {

    //用户名
    @NotBlank(message = "用户名不能为空")
    private String username;

    //密码（明文只在这一次传输里出现，库里存的是 BCrypt 哈希）
    @NotBlank(message = "密码不能为空")
    private String password;
}
