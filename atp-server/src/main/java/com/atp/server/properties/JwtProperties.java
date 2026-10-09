package com.atp.server.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 相关配置属性。
 */
@Data
@Component // 声明这是一个组件
@ConfigurationProperties(prefix = "atp.jwt")
public class JwtProperties {
    private String adminTokenName; // 前端传 token 的请求头名
    private String adminSecretKey; // 后台生成 token 的签名密钥，至少 32 字节
    private Long adminTtl; // token 有效期, 毫秒数
}
