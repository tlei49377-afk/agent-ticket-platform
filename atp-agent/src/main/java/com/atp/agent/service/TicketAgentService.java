package com.atp.agent.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

/**
 * 工单 Agent 的声明式接口。
 *
 * wiringMode = EXPLICIT 表示“注解里写了才装配”，所以 tools 里必须
 * 逐个列出 Spring Bean 名，写错启动就报找不到 Bean。
 *
 * 没有声明 contentRetriever —— RAG 走工具方式调用，不走自动检索增强。
 */
@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "openAiChatModel",
        streamingChatModel = "openAiStreamingChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        tools = {"ticketToolFacade"}
)
public interface TicketAgentService {

    /**
     * 进行一轮流式对话。
     *
     * @param memoryId 会话记忆 ID，格式为 userId:sessionId
     * @param message 用户消息
     * @return 模型流式输出
     */
    @SystemMessage(fromResource = "system.txt")
    Flux<String> chat(
            @MemoryId String memoryId,
            @UserMessage String message);
}