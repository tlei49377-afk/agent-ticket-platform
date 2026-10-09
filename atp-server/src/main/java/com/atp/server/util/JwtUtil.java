package com.atp.server.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 签发与解析。
 */
public class JwtUtil {
    /**
     * 生成 token
     *
     * @param secretKey 密钥
     * @param ttlMillis 有效期毫秒
     * @param claims    要放进去的自定义字段（如 userId、username）
     */
    public static String createJWT(String secretKey, long ttlMillis, Map<String, Object> claims){
        // 当前时间戳（毫秒），作为签发基准
        long now = System.currentTimeMillis();
        return Jwts.builder()
                // 写入自定义载荷（如 userId、username）
                .setClaims(claims)
                // 签发时间
                .setIssuedAt(new Date(now))
                // 过期时间 = 签发时间 + 有效期
                .setExpiration(new Date(now + ttlMillis))
                // 使用 HS256 算法，以密钥字节进行签名
                .signWith(SignatureAlgorithm.HS256, secretKey.getBytes(StandardCharsets.UTF_8))
                // 生成最终的紧凑型 JWT 字符串
                .compact();
    }

    /**
     * 解析 token。签名不对或已过期会抛异常。
     *
     * @param secretKey 密钥，需与签发时一致
     * @param token     待解析的 JWT 字符串
     * @return 载荷中的声明（claims）
     */
    public static Claims parseJWT(String secretKey, String token) {
        return Jwts.parser()
                // 使用与签发时相同的密钥字节作为验签密钥
                .setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8))
                // 解析并校验签名（签名不合法或已过期会抛异常）
                .parseClaimsJws(token)
                // 返回载荷中的声明
                .getBody();
    }
}
