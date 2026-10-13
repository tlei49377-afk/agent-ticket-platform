package com.atp.pojo.entity.ticket;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单附件。一个文件一行，文件本身存在磁盘上，这里只记元数据。
 */
@Data
public class TicketAttachment {

    private Long id; // 主键
    private Long ticketId; // 工单 ID
    private Long messageId; // 所属消息 ID，可为空
    private String fileName; // 原始文件名
    private String fileUrl; // 访问 URL
    private Long fileSize; // 大小（字节）
    private String fileType; // MIME 类型
    private Long uploadBy; // 上传人
    private LocalDateTime createTime; // 创建时间
    private Integer deleted; // 逻辑删除：0 未删 1 已删
}
