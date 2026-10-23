package com.atp.server.controller;

import com.atp.agent.rag.HybridRetriever;
import com.atp.agent.rag.RetrievalDebugResult;
import com.atp.agent.rag.RetrievalMode;
import com.atp.common.annotation.RequirePermission;
import com.atp.common.result.Result;
import com.atp.pojo.dto.kb.KbRetrieveRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识库检索调试接口。
 */
@RestController
@RequestMapping("/kb")
@RequiredArgsConstructor
public class KbRetrievalController {

    // 知识库检索器
    private final HybridRetriever retriever;

    /**
     * 执行一次可观察的检索，用于调参与排错。
     *
     * @param request 检索参数
     * @return 检索结果及各路明细
     */
    @PostMapping("/test-retrieve")
    @RequirePermission("kb:read")
    public Result<RetrievalDebugResult> testRetrieve(@Valid @RequestBody KbRetrieveRequest request) {
        int topK = request.getTopK() == null ? 5 : request.getTopK();
        RetrievalMode mode = RetrievalMode.from(request.getMode());
        return Result.success(retriever.retrieveDebug(request.getQuery(), null, topK, mode));
    }
}
