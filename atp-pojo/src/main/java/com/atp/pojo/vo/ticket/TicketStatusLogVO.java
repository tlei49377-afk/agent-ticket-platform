package com.atp.pojo.vo.ticket;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 状态变更历史的一条。operatorName 是关联 sys_user 查出来的。
 */
@Data
public class TicketStatusLogVO {

    //日志 ID
    private Long id;

    //原状态，建单那一条为空
    private Integer fromStatus;

    //变更后状态
    private Integer toStatus;

    //操作人 ID
    private Long operatorId;

    //操作人姓名，系统自动流转时为空
    private String operatorName;

    //变更原因
    private String reason;

    //变更时间
    private LocalDateTime createTime;
}
