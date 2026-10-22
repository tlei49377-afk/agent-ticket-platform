package com.atp.agent.service;

import com.atp.agent.mapper.KbChunkMapper;
import com.atp.agent.mapper.KbDocumentMapper;
import com.atp.pojo.entity.kb.KbChunk;
import com.atp.pojo.entity.kb.KbDocument;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 知识库文档入库服务。
 *
 * <p>负责文件解析、分块、内容去重、向量化和分块元数据落库。外部 embedding
 * 调用不能包在一个长数据库事务中，因此这里通过状态字段显式推进：
 * {@code pending -> processing -> ready/failed}。</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KbIngestService {

    /** 单个分块的目标字符数。 */
    private static final int CHUNK_SIZE = 500;

    /** 相邻分块的重叠字符数。 */
    private static final int CHUNK_OVERLAP = 100;

    /** 单次调用 embedding 的最大分块数。 */
    private static final int EMBED_BATCH_SIZE = 20;

    /** 估算 token 时每个 token 约对应的字符数。 */
    private static final int CHARS_PER_TOKEN = 4;

    /** 错误信息最大长度，与 kb_document.error_msg 列宽一致。 */
    private static final int ERROR_MSG_MAX = 500;

    private final KbDocumentMapper documentMapper;
    private final KbChunkMapper chunkMapper;

    /** EmbeddingModel 由 LangChain4j starter 根据配置创建。 */
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;

    /** Redis Stack 向量库 Bean；未启用向量库时允许应用启动，但入库会明确失败。 */
    private final ObjectProvider<EmbeddingStore<TextSegment>> embeddingStoreProvider;

    /** 写入 kb_chunk.embedding_model 的模型名。 */
    @Value("${langchain4j.open-ai.embedding-model.model-name:text-embedding-v3}")
    private String embeddingModelName;

    /**
     * 异步处理一个知识库文档。
     *
     * @param documentId 文档 ID
     */
    @Async("kbIngestExecutor")
    public void ingest(Long documentId) {
        KbDocument doc = documentMapper.getById(documentId);
        if (doc == null) {
            log.warn("知识库文档不存在，跳过入库：documentId={}", documentId);
            return;
        }

        documentMapper.updateStatus(documentId, "processing", null, null);
        try {
            doIngest(doc);
            int totalChunks = chunkMapper.countByDocument(documentId);
            documentMapper.updateStatus(documentId, "ready", totalChunks, null);
            log.info("知识库文档入库完成：documentId={}, chunks={}", documentId, totalChunks);
        } catch (Exception e) {
            log.error("知识库文档入库失败：documentId={}", documentId, e);
            documentMapper.updateStatus(documentId, "failed", null, truncate(e.getMessage()));
        }
    }

    /**
     * 执行解析、切分、向量化和落库。
     *
     * @param doc 文档实体
     * @throws Exception 文件不存在、解析失败、向量化失败等
     */
    private void doIngest(KbDocument doc) throws Exception {
        EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
        EmbeddingStore<TextSegment> embeddingStore = embeddingStoreProvider.getIfAvailable();
        if (embeddingModel == null) {
            throw new IllegalStateException("EmbeddingModel 未配置，请检查 langchain4j.open-ai.embedding-model");
        }
        if (embeddingStore == null) {
            throw new IllegalStateException("EmbeddingStore 未启用，请启动 Redis Stack 并开启 langchain4j.community.redis");
        }

        Document document = loadDocument(doc);
        DocumentSplitter splitter = DocumentSplitters.recursive(CHUNK_SIZE, CHUNK_OVERLAP);
        List<TextSegment> segments = splitter.split(document);
        if (segments.isEmpty()) {
            throw new IllegalStateException("文档解析后没有可入库内容");
        }

        List<String> hashes = segments.stream().map(segment -> sha256(segment.text())).toList();
        Set<String> existingHashes = hashes.isEmpty()
                ? Set.of()
                : new HashSet<>(chunkMapper.filterExistingHashes(hashes));

        List<TextSegment> segmentsToEmbed = new ArrayList<>();
        List<KbChunk> chunksToSave = new ArrayList<>();
        Set<String> batchHashes = new HashSet<>();
        int seq = 0;
        for (TextSegment segment : segments) {
            seq++;
            String hash = sha256(segment.text());
            if (existingHashes.contains(hash) || !batchHashes.add(hash)) {
                continue;
            }

            Metadata metadata = new Metadata();
            metadata.put("document_id", doc.getId());
            metadata.put("doc_title", doc.getTitle());
            metadata.put("seq", seq);
            TextSegment enriched = TextSegment.from(segment.text(), metadata);

            KbChunk chunk = new KbChunk();
            chunk.setDocumentId(doc.getId());
            chunk.setSeq(seq);
            chunk.setContent(segment.text());
            chunk.setTokenCount(Math.max(1, segment.text().length() / CHARS_PER_TOKEN));
            chunk.setContentHash(hash);
            chunk.setEmbeddingModel(embeddingModelName);
            segment.metadata().toMap().forEach((key, value) -> {
                if ("page_number".equals(key) && value instanceof Integer pageNo) {
                    chunk.setPageNo(pageNo);
                }
            });

            segmentsToEmbed.add(enriched);
            chunksToSave.add(chunk);
        }

        if (segmentsToEmbed.isEmpty()) {
            log.info("文档 {} 的全部内容均已存在，无需重复向量化", doc.getId());
            return;
        }

        for (int from = 0; from < segmentsToEmbed.size(); from += EMBED_BATCH_SIZE) {
            int to = Math.min(from + EMBED_BATCH_SIZE, segmentsToEmbed.size());
            List<TextSegment> batchSegments = segmentsToEmbed.subList(from, to);
            List<KbChunk> batchChunks = chunksToSave.subList(from, to);

            List<Embedding> embeddings = embeddingModel.embedAll(batchSegments).content();
            List<String> vectorIds = embeddingStore.addAll(embeddings, batchSegments);
            if (vectorIds.size() != batchChunks.size()) {
                throw new IllegalStateException("向量库返回的 ID 数量与分块数量不一致");
            }
            for (int i = 0; i < batchChunks.size(); i++) {
                batchChunks.get(i).setVectorId(vectorIds.get(i));
            }
            chunkMapper.insertBatch(batchChunks);
        }
    }

    /**
     * 从本地文件系统加载文档。
     *
     * <p>上传后的文件是普通磁盘文件，不是 classpath 资源，不能用
     * {@code ClassPathDocumentLoader}。</p>
     *
     * @param doc 文档实体
     * @return LangChain4j 文档对象
     * @throws Exception 文件不存在或格式不支持
     */
    private Document loadDocument(KbDocument doc) throws Exception {
        if (doc.getFilePath() == null || doc.getFilePath().isBlank()) {
            throw new IllegalStateException("文档 file_path 为空");
        }

        Path path = Paths.get(doc.getFilePath()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("文档文件不存在：" + path);
        }

        String extension = extensionOf(path.getFileName().toString());
        if ("pdf".equals(extension)) {
            try (InputStream input = Files.newInputStream(path)) {
                return new ApachePdfBoxDocumentParser().parse(input);
            }
        }
        if ("txt".equals(extension) || "md".equals(extension)) {
            return Document.from(Files.readString(path, StandardCharsets.UTF_8));
        }
        throw new IllegalArgumentException("不支持的文档类型：" + extension);
    }

    /**
     * 取文件扩展名。
     *
     * @param fileName 文件名
     * @return 小写扩展名；没有扩展名时返回空字符串
     */
    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 计算文本的 SHA-256。
     *
     * @param text 文本
     * @return 64 位十六进制摘要
     */
    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("计算内容哈希失败", e);
        }
    }

    /**
     * 截断错误信息，避免超过数据库列宽。
     *
     * @param message 原始错误信息
     * @return 最多 500 字符的错误信息
     */
    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= ERROR_MSG_MAX ? message : message.substring(0, ERROR_MSG_MAX);
    }
}