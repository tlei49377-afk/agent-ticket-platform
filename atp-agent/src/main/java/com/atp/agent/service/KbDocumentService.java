package com.atp.agent.service;

import com.atp.common.result.PageResult;
import com.atp.pojo.query.KbDocumentPageQueryDTO;
import com.atp.pojo.vo.kb.KbDocumentVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 知识库文档管理服务。
 */
public interface KbDocumentService {

    /**
     * 上传文档并触发异步入库。
     *
     * @param file 上传文件，支持 pdf/md/txt
     * @param title 文档标题；为空时使用文件名
     * @param userId 上传人 ID
     * @return 新文档 ID
     * @throws IOException 文件保存失败
     */
    Long upload(MultipartFile file, String title, Long userId) throws IOException;

    /**
     * 异步触发指定文档入库。
     *
     * @param documentId 文档 ID
     */
    void ingestAsync(Long documentId);

    /**
     * 分页查询知识库文档。
     *
     * @param query 分页与筛选条件
     * @return 文档分页结果
     */
    PageResult<KbDocumentVO> page(KbDocumentPageQueryDTO query);

    /**
     * 删除文档、分块和对应向量。
     *
     * @param documentId 文档 ID
     */
    void delete(Long documentId);
}