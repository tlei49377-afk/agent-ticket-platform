package com.atp.server.controller;

import com.atp.common.annotation.RequirePermission;
import com.atp.common.result.Result;
import com.atp.pojo.vo.sys.UserVO;
import com.atp.server.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户查询接口，当前只开放工单指派所需的精简列表。
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final SysUserService sysUserService;

    /**
     * 全部可选用户，供指派工单和维护工单筛选项使用。
     *
     * @return 用户列表
     */
    @GetMapping("/all")
    @RequirePermission("user:list")
    public Result<List<UserVO>> all() {
        return Result.success(sysUserService.listAllUsers());
    }
}
