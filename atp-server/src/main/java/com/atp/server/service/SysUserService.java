package com.atp.server.service;

import com.atp.pojo.dto.sys.LoginDTO;
import com.atp.pojo.vo.sys.LoginVO;

public interface SysUserService {

    LoginVO login(LoginDTO dto);
}
