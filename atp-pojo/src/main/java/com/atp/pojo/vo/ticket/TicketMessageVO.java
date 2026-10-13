package com.atp.pojo.vo.ticket;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 一条工单消息。senderName 是关联 sys_user 查出来的。
 */
@Data
public class TicketMessageVO {

    //消息 ID
    private Long id;

    //发送人角色：requester/agent/ai/system
    private String role;

    //发送人 ID，system 和 ai 可以为空
    private Long senderId;

    //发送人姓名
    private String senderName;

    //内容
    private String content;

    //可见性：public 用户可见 / internal 内部备注
    private String visible;

    //创建时间
    private LocalDateTime createTime;
}
