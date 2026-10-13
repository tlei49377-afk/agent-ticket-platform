package com.atp.pojo.dto.ticket;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 流转状态入参。对应前端 PUT /tickets/{id}/status。
 */
@Data
public class TicketTransitDTO {

    //要流转到哪个状态：1 待受理 2 处理中 3 已挂起 4 待用户确认 5 已解决 6 已关闭
    //能不能过去由状态机判断，不是前端说了算
    @NotNull(message = "目标状态不能为空")
    private Integer targetStatus;

    //流转原因，会记进状态变更历史
    private String reason;
}
