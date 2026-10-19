package com.atp.pojo.vo.agent;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话里的一条消息，只给前端渲染用。
 */
@Data
public class MessageVO {

    //消息 ID
    private Long id;

    //发送方：user / ai / tool / system
    private String role;

    //内容
    private String content;

    //创建时间
    private LocalDateTime createTime;
}
