package com.atp.server.service.impl;

import com.alibaba.fastjson2.JSON;
import com.atp.server.mapper.PermissionMapper;
import com.atp.server.service.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
public class PermissionServiceImpl implements PermissionService {
    // 缓存key前缀
    private static final String CACHE_KEY_PREFIX = "perm:user:";
    // 缓存有效期
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    @Autowired
    private PermissionMapper permissionMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Qualifier("redisTemplate")
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public List<String> listCodesByUserId(Long userId) {
        String key = CACHE_KEY_PREFIX + userId;
        String json = redisTemplate.opsForValue().get(key).toString();
        if (json != null) {
            return JSON.parseArray(json, String.class);
        }

        log.info("权限缓存未命中，查库：userId={}", userId);
        List<String> codes = permissionMapper.listCodesByUserId(userId);

        // 查不到也缓存空列表，防止缓存穿透
        redisTemplate.opsForValue().set(key, JSON.toJSONString(codes), CACHE_TTL);
        return codes;
    }

    @Override
    public void evictCache(Long userId) {
        redisTemplate.delete(CACHE_KEY_PREFIX + userId);
    }
}
