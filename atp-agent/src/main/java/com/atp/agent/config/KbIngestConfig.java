package com.atp.agent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 知识库异步入库线程池配置。
 */
@Configuration
@EnableAsync
public class KbIngestConfig {

    /**
     * 知识库解析与向量化线程池。
     *
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
}