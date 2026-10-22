package com.atp.agent.mapper;

import com.atp.pojo.entity.kb.KbDocument;
import com.atp.pojo.query.KbDocumentPageQueryDTO;
import com.atp.pojo.vo.kb.KbDocumentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识库文档访问。
 */
@Mapper
public interface KbDocumentMapper {

    /**
     * 新增知识库文档。
     *
     * @param document 文档实体
     * @return 影响行数
     */
    int insert(KbDocument document);

    /**
     * 按主键查询未删除文档。
     *
     * @param id 文档 ID
     * @return 文档实体；不存在时返回 null
     */
    KbDocument getById(@Param("id") Long id);

    /**
     * 分页查询文档。
     *
     * @param query 分页与筛选条件
     * @return 文档列表
     */
    List<KbDocumentVO> page(KbDocumentPageQueryDTO query);

    /**
     * 按状态查询待处理文档。
     *
     * @param status 文档状态
     * @return 文档列表
     */
    List<KbDocument> listByStatus(@Param("status") String status);

    /**
     * 更新文档状态、分块数和错误信息。
     *
     * @param id 文档 ID
     * @param status 新状态
     * @param chunkCount 分块数量；null 表示保持原值
     * @param errorMsg 错误信息；成功时为 null
     * @return 影响行数
     */
    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("chunkCount") Integer chunkCount,
                     @Param("errorMsg") String errorMsg);

    /**
     * 逻辑删除文档。
     *
     * @param id 文档 ID
     * @return 影响行数
     */
    int delete(@Param("id") Long id);
}