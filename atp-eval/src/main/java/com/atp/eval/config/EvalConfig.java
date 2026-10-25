package com.atp.eval.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 评测任务线程池。
 *
 * <p>评测需要连续调用模型，默认单线程顺序执行，避免 24 条用例同时请求
 * 把外部模型配额打满，也避免结果写入顺序不稳定。</p>
 */
@Configuration
public class EvalConfig {

    /**
     * 评测专用执行器。
     *
     * @return 单线程任务执行器
     */
    @Bean(name = "evalExecutor")
    public Executor evalExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(4);
        executor.setThreadNamePrefix("eval-runner-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
