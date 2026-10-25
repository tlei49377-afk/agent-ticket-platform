package com.atp.server.controller;

import com.atp.agent.service.KbDocumentService;
import com.atp.common.annotation.RequirePermission;
import com.atp.common.exception.BaseException;
import com.atp.common.result.PageResult;
import com.atp.common.result.Result;
import com.atp.pojo.query.KbDocumentPageQueryDTO;
import com.atp.pojo.vo.kb.KbDocumentDetailVO;
import com.atp.pojo.vo.kb.KbDocumentVO;
import com.atp.server.interceptor.JwtTokenInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 知识库文档管理接口。
 *
 * <p>检索调试接口在 KbRetrievalController；这里负责文档列表、正文查看和导入。</p>
 */
@RestController
@RequestMapping("/kb/documents")
@RequiredArgsConstructor
public class KbDocumentController {

    private final KbDocumentService documentService;

    private final HttpServletRequest request;

    /**
     * 分页查询全部知识库文档。
     *
     * @param query 标题关键词、类型、状态和分页参数
     * @return 文档分页结果
     */
    @GetMapping
    @RequirePermission("kb:read")
    public Result<PageResult<KbDocumentVO>> page(KbDocumentPageQueryDTO query) {
        return Result.success(documentService.page(query));
    }

    /**
     * 查看文档全文和分块。
     *
     * @param id 文档 ID
     * @return 文档详情
     */
    @GetMapping("/{id}/content")
    @RequirePermission("kb:read")
    public Result<KbDocumentDetailVO> content(@PathVariable Long id) {
        return Result.success(documentService.detail(id));
    }

    /**
     * 导入 PDF、Markdown 或 TXT 文件。
     *
     * <p>文件先落盘并创建 pending 文档，随后由 KbIngestService 异步切分和向量化。
     * 导入属于知识库管理操作，只有 kb:manage 权限可以调用。</p>
     *
     * @param file 上传文件
     * @param title 可选标题，为空时使用文件名
     * @return 新文档 ID
     */
    @PostMapping("/upload")
    @RequirePermission("kb:manage")
    public Result<Long> upload(@RequestParam("file") MultipartFile file,
                               @RequestParam(value = "title", required = false) String title) {
        Long userId = (Long) request.getAttribute(JwtTokenInterceptor.ATTR_CALLER_ID);
        try {
            return Result.success(documentService.upload(file, title, userId));
        } catch (IOException e) {
            throw new BaseException("知识库文件保存失败");
        }
    }
}
