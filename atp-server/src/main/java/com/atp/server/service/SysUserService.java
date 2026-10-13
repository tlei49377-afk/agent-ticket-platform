package com.atp.server.service;

import com.atp.pojo.dto.sys.LoginDTO;
import com.atp.pojo.vo.sys.LoginVO;

/**
 * 系统用户。登录与令牌签发。
 */
public interface SysUserService {

    /**
     * 登录校验，通过则签发令牌
     * @param dto
     * @return
     */
    LoginVO login(LoginDTO dto);
}
