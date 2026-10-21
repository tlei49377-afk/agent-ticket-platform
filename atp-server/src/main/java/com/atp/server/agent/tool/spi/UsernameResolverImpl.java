package com.atp.server.agent.tool.spi;

import com.atp.agent.tool.spi.UsernameResolver;
import com.atp.pojo.entity.sys.SysUser;
import com.atp.server.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * UsernameResolver 的 atp-server 实现。
 *
 * <p>优先返回姓名，姓名为空时退回登录名，保证审计日志也有可读身份。</p>
 */
@Component
@RequiredArgsConstructor
public class UsernameResolverImpl implements UsernameResolver {

    private final SysUserMapper sysUserMapper;

    /**
     * 按用户 ID 查询展示名。
     *
     * @param userId 用户 ID
     * @return 姓名、登录名或兜底标识
     */
    @Override
    public String getUsername(Long userId) {
        if (userId == null) {
            return "未知用户";
        }
        SysUser user = sysUserMapper.getById(userId);
        if (user == null) {
            return "用户" + userId;
        }
        if (StringUtils.hasText(user.getName())) {
            return user.getName();
        }
        if (StringUtils.hasText(user.getUsername())) {
            return user.getUsername();
        }
        return "用户" + userId;
    }
}