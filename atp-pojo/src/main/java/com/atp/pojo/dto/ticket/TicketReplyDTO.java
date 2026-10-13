package com.atp.pojo.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 回复工单入参。对应前端 POST /tickets/{id}/replies。
 */
@Data
public class TicketReplyDTO {

    //回复内容
    @NotBlank(message = "回复内容不能为空")
    @Size(max = 5000, message = "回复内容最长 5000 个字符")
    private String content;

    //可见性：public 客户可见 / internal 仅内部可见，不传按 public
    //提交人就算传 internal 也会被降级成 public，内部备注不进客户视野
    private String visible;
}
