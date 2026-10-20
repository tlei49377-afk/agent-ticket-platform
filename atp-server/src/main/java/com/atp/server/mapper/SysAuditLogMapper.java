package com.atp.server.mapper;

import com.atp.pojo.entity.sys.SysAuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志访问。只增不改，是回溯"谁在什么时候做了什么"的依据。
 */
@Mapper
public interface SysAuditLogMapper {

    /**
     * 记一条审计日志，自增主键回填到入参的 id 上
     * @param log 审计日志
     * @return 影响行数
     */
    int insert(SysAuditLog log);
}