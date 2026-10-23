package com.atp.server.controller;

import com.atp.common.constant.MessageConstant;
import com.atp.common.exception.BaseException;
import com.atp.common.result.Result;
import com.atp.pojo.entity.ticket.TicketAttachment;
import com.atp.server.interceptor.JwtTokenInterceptor;
import com.atp.server.mapper.TicketAttachmentMapper;
import com.atp.server.mapper.TicketMapper;
import com.atp.server.properties.UploadProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 附件上传。文件存磁盘，元数据存 ticket_attachment，返回可直接访问的 URL。
 */
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    // 扩展名白名单。不在名单里的一律拒绝，别信前端传来的文件名
    private static final Set<String> ALLOWED_EXT =
            Set.of("jpg", "jpeg", "png", "gif", "pdf", "txt", "md", "docx", "xlsx");

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final TicketAttachmentMapper attachmentMapper;
    private final TicketMapper ticketMapper;
    private final UploadProperties uploadProperties;

    /**
     * 上传附件：校验类型 → 落到磁盘 → 元数据入库 → 返回可访问的 URL
     * @param file
     * @param ticketId
     * @param request
     * @return
     */
    @PostMapping
    public Result<TicketAttachment> upload(@RequestParam("file") MultipartFile file,
                                           @RequestParam("ticketId") Long ticketId,
                                           HttpServletRequest request) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BaseException(MessageConstant.FILE_UPLOAD_FAILED);
        }
        if (ticketMapper.getById(ticketId) == null) {
            throw new BaseException(MessageConstant.TICKET_NOT_FOUND);
        }

        String originalName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = extensionOf(originalName);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BaseException(MessageConstant.FILE_TYPE_NOT_ALLOWED);
        }

        // 存储名用 UUID：原始文件名可能带 ../ 或 .jsp 这类东西，绝不能拿它拼路径
        String day = LocalDate.now().format(DAY);
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;

        Path root = Paths.get(uploadProperties.getUploadDir()).toAbsolutePath().normalize();
        Path targetDir = root.resolve(day);
        Files.createDirectories(targetDir);
        file.transferTo(targetDir.resolve(storedName).toFile());

        TicketAttachment attachment = new TicketAttachment();
        attachment.setTicketId(ticketId);
        attachment.setFileName(originalName);
        attachment.setFileUrl(uploadProperties.getUrlPrefix() + "/" + day + "/" + storedName);
        attachment.setFileSize(file.getSize());
        attachment.setFileType(file.getContentType());
        attachment.setUploadBy((Long) request.getAttribute(JwtTokenInterceptor.ATTR_CALLER_ID));
        attachmentMapper.insert(attachment);
        return Result.success(attachment);
    }

    /**
     * 取扩展名，统一转小写再和名单比对
     * @param fileName
     * @return
     */
    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
