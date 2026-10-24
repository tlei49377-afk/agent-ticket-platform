package com.atp.server.service;

import com.atp.pojo.dto.sys.LoginDTO;
import com.atp.pojo.vo.sys.LoginVO;
import com.atp.pojo.vo.sys.UserProfileVO;
import com.atp.pojo.vo.sys.UserVO;

import java.util.List;

/**
 * 系统用户。登录、令牌签发和前端所需的用户查询。
 */
public interface SysUserService {

    /**
     * 登录校验，通过则签发令牌。
     *
     * @param dto 登录参数
     * @return 登录结果
     */
    LoginVO login(LoginDTO dto);

    /**
     * 查询当前登录人的角色与权限。
     *
     * @param userId 当前用户 ID
     * @return 用户资料
     */
    UserProfileVO currentUser(Long userId);

    /**
     * 查询可指派用户列表。
     *
     * @return 用户列表
     */
    List<UserVO> listAllUsers();
}
