package com.atp.server.mapper;

import com.atp.pojo.entity.ticket.SlaPolicy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * SLA 策略访问。
 */
@Mapper
public interface SlaPolicyMapper {

    /**
     * 全部策略
     * @return
     */
    List<SlaPolicy> list();

    /**
     * 按 ID 查策略
     * @param id
     * @return
     */
    SlaPolicy getById(@Param("id") Long id);

    /**
     * 建单时按优先级取策略，取不到就没法算截止时间
     * @param priority
     * @return
     */
    SlaPolicy getEnabledByPriority(@Param("priority") Integer priority);

    /**
     * 新增策略，主键回填
     * @param policy
     * @return
     */
    int insert(SlaPolicy policy);

    /**
     * 按主键更新非空字段
     * @param policy
     * @return
     */
    int update(SlaPolicy policy);

    /**
     * 逻辑删除
     * @param id
     * @return
     */
    int deleteById(@Param("id") Long id);
}
