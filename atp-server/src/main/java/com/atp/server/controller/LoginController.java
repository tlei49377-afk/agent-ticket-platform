package com.atp.server.controller;

import com.atp.common.result.Result;
import com.atp.pojo.dto.sys.LoginDTO;
import com.atp.pojo.vo.sys.LoginVO;
import com.atp.pojo.vo.sys.UserProfileVO;
import com.atp.server.interceptor.JwtTokenInterceptor;
import com.atp.server.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录与当前用户信息接口。
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class LoginController {

    private final SysUserService sysUserService;

    private final HttpServletRequest request;

    /**
     * 登录：校验账号密码，返回用户信息和令牌。
     *
     * @param dto 登录参数
     * @return 登录结果
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        log.info("用户登录: {}", dto.getUsername());
        return Result.success(sysUserService.login(dto));
    }

    /**
     * 查询当前登录人的角色与权限，刷新页面后前端用它恢复菜单。
     *
     * @return 当前用户资料
     */
    @GetMapping("/auth/me")
    public Result<UserProfileVO> me() {
        Long userId = (Long) request.getAttribute(JwtTokenInterceptor.ATTR_CALLER_ID);
        return Result.success(sysUserService.currentUser(userId));
    }

    /**
     * 退出登录。令牌是无状态的，前端删掉本地保存的即可。
     *
     * @return 成功结果
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        log.info("用户退出登录");
        return Result.success();
    }
}
