package com.atp.agent.tool.spi;

/**
 * 用户名解析 SPI。
 *
 * <p>atp-agent 不能反向依赖 atp-server，因此工具门面只依赖这个接口；
 * 真正的用户表查询由 atp-server 提供实现。</p>
 */
public interface UsernameResolver {

    /**
     * 按用户 ID 查询用于展示和审计的姓名。
     *
     * @param userId 用户 ID
     * @return 用户姓名；用户不存在时返回可读的兜底值
     */
    String getUsername(Long userId);
}