package com.atp.agent.config;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

/**
 * 会话记忆装配。
 *
 * <p>注意：langchain4j-community-redis-spring-boot-starter(1.0.1-beta6) 里的
 * org.springframework.boot.autoconfigure.AutoConfiguration.imports 只注册了
 * RedisEmbeddingStoreAutoConfiguration（向量库），并不会提供 ChatMemoryStore Bean，
 * 所以这里必须显式声明一个，否则启动时注入 ChatMemoryStore 会报 NoSuchBeanDefinitionException。
 *
 * <p>实现直接复用 Spring 的 {@link StringRedisTemplate}，从而沿用 application-dev.yml 中
 * spring.data.redis 的库号、密码与连接池配置，避免再单独维护一套 Jedis 连接。
 */
@Configuration
public class AgentMemoryConfig {

    @Value("${atp.agent.memory-window-size:30}")
    private int windowSize;

    /**
     * 记忆存储：每个会话一个 Redis String，内容是消息列表的 JSON
     * @param stringRedisTemplate Spring 的 Redis 客户端
     * @param keyPrefix key 前缀，默认 atp:agent:memory:
     * @return ChatMemoryStore
     */
    @Bean
    public ChatMemoryStore redisChatMemoryStore(
            StringRedisTemplate stringRedisTemplate,
            @Value("${atp.agent.memory-key-prefix:atp:agent:memory:}") String keyPrefix) {
        return new SpringRedisChatMemoryStore(stringRedisTemplate, keyPrefix);
    }

    /**
     * 按 memoryId 给每个会话一份独立的窗口记忆，条数上限 windowSize
     * @param redisChatMemoryStore 上面的记忆存储
     * @return ChatMemoryProvider
     */
    @Bean
    public ChatMemoryProvider chatMemoryProvider(ChatMemoryStore redisChatMemoryStore) {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(windowSize)
                .chatMemoryStore(redisChatMemoryStore)
                .build();
    }

    /**
     * 基于 Spring Redis 的 {@link ChatMemoryStore} 实现：每个会话消息以 JSON 形式存于一个 Redis String。
     */
    static class SpringRedisChatMemoryStore implements ChatMemoryStore {

        private final StringRedisTemplate redis;
        private final String keyPrefix;

        SpringRedisChatMemoryStore(StringRedisTemplate redis, String keyPrefix) {
            this.redis = redis;
            this.keyPrefix = keyPrefix;
        }

        @Override
        public List<ChatMessage> getMessages(Object memoryId) {
            String json = redis.opsForValue().get(key(memoryId));
            return json == null ? List.of() : ChatMessageDeserializer.messagesFromJson(json);
        }

        @Override
        public void updateMessages(Object memoryId, List<ChatMessage> messages) {
            redis.opsForValue().set(key(memoryId), ChatMessageSerializer.messagesToJson(messages));
        }

        @Override
        public void deleteMessages(Object memoryId) {
            redis.delete(key(memoryId));
        }

        private String key(Object memoryId) {
            return keyPrefix + memoryId;
        }
    }
}