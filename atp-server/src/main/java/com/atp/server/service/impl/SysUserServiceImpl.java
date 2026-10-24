package com.atp.server.service.impl;

import com.atp.common.constant.MessageConstant;
import com.atp.common.exception.BaseException;
import com.atp.pojo.dto.sys.LoginDTO;
import com.atp.pojo.entity.sys.SysUser;
import com.atp.pojo.vo.sys.LoginVO;
import com.atp.pojo.vo.sys.UserProfileVO;
import com.atp.pojo.vo.sys.UserVO;
import com.atp.server.mapper.SysUserMapper;
import com.atp.server.properties.JwtProperties;
import com.atp.server.service.PermissionService;
import com.atp.server.service.SysUserService;
import com.atp.server.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统用户服务实现。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper sysUserMapper;

    private final JwtProperties jwtProperties;

    private final PermissionService permissionService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 校验用户名密码，通过后签发令牌并更新最后登录时间。
     *
     * @param dto 登录参数
     * @return 登录结果
     */
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

    /**
     * 查询当前登录人的角色和权限码。
     *
     * @param userId 当前用户 ID
     * @return 用户资料
     */
    @Override
    public UserProfileVO currentUser(Long userId) {
        SysUser user = sysUserMapper.getById(userId);
        if (user == null) {
            throw new BaseException(MessageConstant.USER_NOT_FOUND);
        }

        UserProfileVO profile = new UserProfileVO();
        profile.setId(user.getId());
        profile.setUsername(user.getUsername());
        profile.setName(user.getName());
        profile.setRoles(sysUserMapper.listRoleCodesByUserId(userId));
        profile.setPermissions(permissionService.listCodesByUserId(userId));
        return profile;
    }

    /**
     * 查询全部未删除用户，供工单指派下拉使用。
     *
     * @return 用户列表
     */
    @Override
    public List<UserVO> listAllUsers() {
        return sysUserMapper.listUsers(null, null);
    }
}
