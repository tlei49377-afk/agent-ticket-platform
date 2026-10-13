package com.atp.server.controller;

import com.atp.common.result.Result;
import com.atp.pojo.dto.sys.LoginDTO;
import com.atp.pojo.vo.sys.LoginVO;
import com.atp.server.service.SysUserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录接口。
 */
@RestController
@Slf4j
public class LoginController {
    @Autowired
    private SysUserService sysUserService;

    /**
     * 登录：校验账号密码，返回用户信息和令牌
     * @param dto
     * @return
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        log.info("用户登录: {}", dto.getUsername());
        return Result.success(sysUserService.login(dto));
    }

    /**
     * 退出登录。令牌是无状态的，前端删掉本地保存的即可
     * @return
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        log.info("用户退出登录");
        return Result.success();
    }
}
