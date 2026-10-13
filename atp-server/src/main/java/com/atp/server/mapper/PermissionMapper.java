package com.atp.server.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 权限码查询。角色变了要记得让缓存失效。
 */
@Mapper
public interface PermissionMapper {
    /**
     * 查这个用户拥有的全部权限码
     * @param userId
     * @return
     */
    List<String> listCodesByUserId(@Param("userId") Long userId);
}
