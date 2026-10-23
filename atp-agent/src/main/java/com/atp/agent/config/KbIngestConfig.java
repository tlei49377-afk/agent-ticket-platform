package com.atp.agent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

import java.util.concurrent.Executor;

/**
 * 知识库异步入库与 Redis 检索基础设施配置。
 */
@Configuration
@EnableAsync
public class KbIngestConfig {

    /**
     * 知识库解析与向量化线程池：
     * 入库属于 IO 密集任务，线程数不需要很大；队列满时由调用线程执行，
     * 避免任务被静默丢弃。
     *
     * @return 异步执行器
     */
    @Bean(name = "kbIngestExecutor")
    public Executor kbIngestExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("kb-ingest-");
        executor.setRejectedExecutionHandler(
                new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 专门连接 Redis Stack 的 Jedis 客户端。
     * Spring 的 StringRedisTemplate 连接的是业务 Redis 6379，而向量库在
     * Redis Stack 6380，因此关键词检索必须使用独立客户端。
     *
     * @param host Redis Stack 主机
     * @param port Redis Stack 端口
     * @param user Redis ACL 用户，可空
     * @param password Redis 密码，可空
     * @return Jedis 客户端
     */
    @Bean(destroyMethod = "close")
    public JedisPooled kbVectorJedis(
            @Value("${langchain4j.community.redis.host:localhost}") String host,
            @Value("${langchain4j.community.redis.port:6380}") int port,
            @Value("${langchain4j.community.redis.user:}") String user,
            @Value("${langchain4j.community.redis.password:}") String password) {
        DefaultJedisClientConfig.Builder config = DefaultJedisClientConfig.builder()
                .connectionTimeoutMillis(3000)
                .socketTimeoutMillis(5000);
        if (StringUtils.hasText(user)) {
            config.user(user);
        }
        if (StringUtils.hasText(password)) {
            config.password(password);
        }
        return new JedisPooled(new HostAndPort(host, port), config.build());
    }
}