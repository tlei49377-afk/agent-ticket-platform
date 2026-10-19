package com.atp.pojo.vo.agent;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话列表项。memoryId 前端要拿着它去拉消息。
 */
@Data
public class SessionVO {

    //会话的 memoryId
    private String memoryId;

    //会话标题
    private String title;

    //最后活跃时间
    private LocalDateTime lastActiveAt;
}