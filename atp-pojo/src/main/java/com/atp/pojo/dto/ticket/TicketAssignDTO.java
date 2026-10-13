package com.atp.pojo.dto.ticket;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 指派/转派入参。对应前端 PUT /tickets/{id}/assignee。
 */
@Data
public class TicketAssignDTO {

    //要指给谁：接手这张单的员工 ID
    @NotNull(message = "处理人不能为空")
    private Long assigneeId;

    //指派原因，会记进状态变更历史
    private String reason;
}
