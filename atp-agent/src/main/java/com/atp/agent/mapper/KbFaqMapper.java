package com.atp.agent.mapper;

import com.atp.pojo.entity.kb.KbFaq;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * FAQ 词条访问。
 */
@Mapper
public interface KbFaqMapper {

    /**
     * 新增 FAQ 词条。
     *
     * @param faq FAQ 实体
     * @return 影响行数
     */
    int insert(KbFaq faq);

    /**
     * 查询启用中的 FAQ。
     *
     * @return FAQ 列表
     */
    List<KbFaq> listEnabled();

    /**
     * 按内容哈希统计已存在条数。
     *
     * @param contentHash 内容哈希
     * @return 已存在条数
     */
    int countByHash(@Param("contentHash") String contentHash);

    /**
     * 增加命中次数。
     *
     * @param id FAQ ID
     * @return 影响行数
     */
    int increaseHit(@Param("id") Long id);
}