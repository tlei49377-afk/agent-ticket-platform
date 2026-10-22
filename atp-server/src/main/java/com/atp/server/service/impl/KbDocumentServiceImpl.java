package com.atp.server.service.impl;

import com.atp.agent.mapper.KbChunkMapper;
import com.atp.agent.mapper.KbDocumentMapper;
import com.atp.agent.service.KbDocumentService;
import com.atp.agent.service.KbIngestService;
import com.atp.common.exception.BaseException;
import com.atp.common.result.PageResult;
import com.atp.pojo.entity.kb.KbChunk;
import com.atp.pojo.entity.kb.KbDocument;
import com.atp.pojo.query.KbDocumentPageQueryDTO;
import com.atp.pojo.vo.kb.KbDocumentVO;
import com.atp.server.properties.UploadProperties;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 知识库文档管理实现。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KbDocumentServiceImpl implements KbDocumentService {

    /** 支持入库的文件扩展名。 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "md", "txt");

    /** 文件目录按天分组。 */
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final KbDocumentMapper documentMapper;
    private final KbChunkMapper chunkMapper;
    private final KbIngestService ingestService;
    private final UploadProperties uploadProperties;
    private final ObjectProvider<EmbeddingStore<TextSegment>> embeddingStoreProvider;

    /**
     * 上传文档并触发异步入库。
     *
     * @param file 上传文件
     * @param title 文档标题
     * @param userId 上传人 ID
     * @return 新文档 ID
     * @throws IOException 文件保存失败
     */
    @Override
    public Long upload(MultipartFile file, String title, Long userId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BaseException("上传文件不能为空");
        }

        String originalName = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
        String extension = extensionOf(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BaseException("知识库只支持 pdf、md、txt 文件");
        }

        String day = LocalDate.now().format(DAY_FORMAT);
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path root = Paths.get(uploadProperties.getUploadDir()).toAbsolutePath().normalize();
        Path targetDir = root.resolve("kb").resolve(day);
        Files.createDirectories(targetDir);
        Path target = targetDir.resolve(storedName);
        file.transferTo(target.toFile());

        KbDocument document = new KbDocument();
        document.setTitle(StringUtils.hasText(title) ? title.trim() : baseName(originalName));
        document.setDocType("file");
        document.setSourceUrl(uploadProperties.getUrlPrefix() + "/kb/" + day + "/" + storedName);
        document.setFilePath(target.toString());
        document.setStatus("pending");
        document.setVersion(1);
        document.setUploadedBy(userId);
        documentMapper.insert(document);

        ingestService.ingest(document.getId());
        return document.getId();
    }

    /**
     * 异步触发指定文档入库。
     *
     * @param documentId 文档 ID
     */
    @Override
    public void ingestAsync(Long documentId) {
        ingestService.ingest(documentId);
    }

    /**
     * 分页查询知识库文档。
     *
     * @param query 分页与筛选条件
     * @return 文档分页结果
     */
    @Override
    public PageResult<KbDocumentVO> page(KbDocumentPageQueryDTO query) {
        PageHelper.startPage(query.getPage(), query.getPageSize());
        List<KbDocumentVO> rows = documentMapper.page(query);
        PageInfo<KbDocumentVO> pageInfo = new PageInfo<>(rows);
        return new PageResult<>(pageInfo.getTotal(), rows);
    }

    /**
     * 删除文档、分块和已写入向量库的向量。
     *
     * @param documentId 文档 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long documentId) {
        KbDocument document = documentMapper.getById(documentId);
        if (document == null) {
            throw new BaseException("知识库文档不存在");
        }

        List<KbChunk> chunks = chunkMapper.listByDocument(documentId);
        EmbeddingStore<TextSegment> embeddingStore = embeddingStoreProvider.getIfAvailable();
        if (embeddingStore != null) {
            List<String> vectorIds = chunks.stream()
                    .map(KbChunk::getVectorId)
                    .filter(StringUtils::hasText)
                    .toList();
            if (!vectorIds.isEmpty()) {
                embeddingStore.removeAll(vectorIds);
            }
        }

        chunkMapper.deleteByDocument(documentId);
        documentMapper.delete(documentId);
        deleteQuietly(document.getFilePath());
    }

    /**
     * 删除本地文件；删除失败只记录日志。
     *
     * @param filePath 文件路径
     */
    private void deleteQuietly(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (Exception e) {
            log.warn("知识库文件删除失败：{}", filePath, e);
        }
    }

    /**
     * 取文件扩展名。
     *
     * @param fileName 文件名
     * @return 小写扩展名
     */
    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 去掉扩展名，作为默认标题。
     *
     * @param fileName 文件名
     * @return 不带扩展名的文件名
     */
    private String baseName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot <= 0 ? fileName : fileName.substring(0, dot);
    }
}