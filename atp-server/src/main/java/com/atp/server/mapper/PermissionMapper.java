package com.atp.server.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PermissionMapper {
    List<String> listCodesByUserId(@Param("userId") Long userId);
}
