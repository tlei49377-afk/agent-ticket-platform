package com.atp.pojo.vo.ticket;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单列表行。字段名是和前端约定死的，改名要同步改前端。
 */
@Data
public class TicketVO {

    //工单 ID
    private Long id;

    //工单号，如 TK202610010001
    private String ticketNo;

    //标题
    private String title;

    //状态码：1 待受理 2 处理中 3 已挂起 4 待用户确认 5 已解决 6 已关闭
    private Integer status;

    //状态中文名，后端翻译好前端直接显示
    private String statusLabel;

    //优先级：1 紧急 2 高 3 中 4 低
    private Integer priority;

    //优先级中文名
    private String priorityLabel;

    //分类 ID
    private Long categoryId;

    //分类名称，关联 ticket_category 查出来的
    private String categoryName;

    //提交人 ID
    private Long requesterId;

    //提交人姓名
    private String requesterName;

    //当前处理人 ID，还没指派时为空
    private Long assigneeId;

    //当前处理人姓名
    private String assigneeName;

    //响应截止时间
    private LocalDateTime respondDeadline;

    //解决截止时间
    private LocalDateTime resolveDeadline;

    //距解决截止还剩多少分钟，负数表示已超期；已解决/已关闭的工单不给这个值
    private Long slaRemainMinutes;

    //创建时间
    private LocalDateTime createTime;

    //更新时间
    private LocalDateTime updateTime;
}
