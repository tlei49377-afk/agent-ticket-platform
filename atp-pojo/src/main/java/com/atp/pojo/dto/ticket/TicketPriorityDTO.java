package com.atp.pojo.dto.ticket;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 调整优先级入参。对应前端 PUT /tickets/{id}/priority。
 */
@Data
public class TicketPriorityDTO {

    //要调成几级：1 紧急 2 高 3 中 4 低
    @NotNull(message = "优先级不能为空")
    @Min(value = 1, message = "优先级只能是 1~4")
    @Max(value = 4, message = "优先级只能是 1~4")
    private Integer targetPriority;

    //调整原因，会记进状态变更历史
    private String reason;
}
