package com.atp.server.config;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

/**
 * 自定义指标。阶段 6 在这里注册 agent/tool/llm 相关指标。
 */
@Configuration
@RequiredArgsConstructor
public class MetricsConfig {

    private final MeterRegistry registry;

}
