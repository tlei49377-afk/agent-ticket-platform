package com.atp.server.service;

import com.atp.pojo.vo.kb.KbReferenceVO;

import java.util.List;

/**
 * 知识库引用查询服务。
 */
public interface KbReferenceService {

    /**
     * 查询一次运行最近一次检索的引用。
     *
     * @param runId 运行 ID
     * @return 引用列表
     */
    List<KbReferenceVO> listByRun(Long runId);
}
