package com.atp.agent.mapper;

import com.atp.pojo.entity.kb.KbRetrievalLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 检索日志访问。日志只增不改。
 */
@Mapper
public interface KbRetrievalLogMapper {

    /**
     * 写入一条检索日志。
     *
     * @param log 检索日志
     * @return 影响行数
     */
    int insert(KbRetrievalLog log);

    /**
     * 查询一次运行的全部检索日志。
     *
     * @param runId 运行 ID
     * @return 按主键升序的日志
     */
    List<KbRetrievalLog> listByRun(@Param("runId") Long runId);

    /**
     * 查询一次运行最近的一条检索日志。
     *
     * @param runId 运行 ID
     * @return 最近一条日志；不存在时返回 null
     */
    KbRetrievalLog getLatestByRun(@Param("runId") Long runId);
}