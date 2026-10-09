package com.atp.server.service.impl;

import com.atp.common.constant.MessageConstant;
import com.atp.common.exception.BaseException;
import com.atp.pojo.dto.sys.LoginDTO;
import com.atp.pojo.entity.sys.SysUser;
import com.atp.pojo.vo.sys.LoginVO;
import com.atp.server.mapper.SysUserMapper;
import com.atp.server.properties.JwtProperties;
import com.atp.server.service.SysUserService;
import com.atp.server.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class SysUserServiceImpl implements SysUserService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private JwtProperties jwtProperties;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public LoginVO login(LoginDTO dto) {
        SysUser user = sysUserMapper.selectByUsername(dto.getUsername());

        // 查不到和密码错返回同一句文案，不给攻击者做用户名枚举的机会
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BaseException(MessageConstant.LOGIN_FAILED);
        }
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new BaseException(MessageConstant.ACCOUNT_DISABLED);
        }

        sysUserMapper.updateLastLoginAt(user.getId());

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        String token = JwtUtil.createJWT(jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(), claims);

        log.info("登录成功，userId={}", user.getId());
        return new LoginVO(user.getId(), user.getUsername(), user.getName(), token);
    }
}
