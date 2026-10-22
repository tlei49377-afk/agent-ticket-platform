package com.atp.agent.mapper;

import com.atp.pojo.entity.kb.KbChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识库分块访问。
 */
@Mapper
public interface KbChunkMapper {

    /**
     * 批量插入分块。
     *
     * @param list 分块列表
     * @return 影响行数
     */
    int insertBatch(@Param("list") List<KbChunk> list);

    /**
     * 查询一个文档的全部分块。
     *
     * @param documentId 文档 ID
     * @return 按序号升序的分块列表
     */
    List<KbChunk> listByDocument(@Param("documentId") Long documentId);

    /**
     * 查询一组内容哈希中已经存在的哈希。
     *
     * @param hashes 待检查的哈希列表，调用方必须保证非空
     * @return 已存在的哈希
     */
    List<String> filterExistingHashes(@Param("hashes") List<String> hashes);

    /**
     * 统计一个文档当前拥有的分块数。
     *
     * @param documentId 文档 ID
     * @return 分块数量
     */
    int countByDocument(@Param("documentId") Long documentId);

    /**
     * 删除一个文档的全部分块。
     *
     * @param documentId 文档 ID
     * @return 影响行数
     */
    int deleteByDocument(@Param("documentId") Long documentId);

    /**
     * 回填向量主键。
     *
     * @param id 分块 ID
     * @param vectorId 向量库中的主键
     * @return 影响行数
     */
    int updateVectorId(@Param("id") Long id, @Param("vectorId") String vectorId);
}