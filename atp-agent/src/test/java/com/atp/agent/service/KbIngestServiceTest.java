package com.atp.agent.service;

import com.atp.agent.mapper.KbChunkMapper;
import com.atp.agent.mapper.KbDocumentMapper;
import com.atp.pojo.entity.kb.KbDocument;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * KbIngestService 基础测试。
 */
class KbIngestServiceTest {

    /**
     * 缺少 EmbeddingModel 时应把文档状态置为 failed，而不是抛出到调用方。
     */
    @Test
    @SuppressWarnings("unchecked")
    void ingest_缺少嵌入模型_应记录失败状态() {
        KbDocumentMapper documentMapper = mock(KbDocumentMapper.class);
        KbChunkMapper chunkMapper = mock(KbChunkMapper.class);
        ObjectProvider<EmbeddingModel> modelProvider = mock(ObjectProvider.class);
        ObjectProvider<EmbeddingStore<TextSegment>> storeProvider = mock(ObjectProvider.class);

        KbDocument document = new KbDocument();
        document.setId(1L);
        document.setTitle("测试文档");
        document.setFilePath("not-used.txt");

        when(documentMapper.getById(1L)).thenReturn(document);
        when(modelProvider.getIfAvailable()).thenReturn(null);

        KbIngestService service = new KbIngestService(
                documentMapper, chunkMapper, modelProvider, storeProvider);

        service.ingest(1L);

        verify(documentMapper).updateStatus(1L, "processing", null, null);
        verify(documentMapper).updateStatus(
                eq(1L), eq("failed"), isNull(), contains("EmbeddingModel"));
    }
}