package com.atp.pojo.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建工单的入参。对应前端 POST /tickets。
 */
@Data
public class TicketCreateDTO {

    //工单标题
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题最长 200 个字符")
    private String title;

    //问题描述，可以只写一句话，后面在消息里补充
    @Size(max = 5000, message = "问题描述最长 5000 个字符")
    private String content;

    //分类 ID，不传就没有分类名
    private Long categoryId;

    //优先级 1 紧急 2 高 3 中 4 低，不传则按分类的默认优先级
    private Integer priority;

    //来源渠道 web/mail/api/phone/ai，不传按 web
    private String channel;
}
