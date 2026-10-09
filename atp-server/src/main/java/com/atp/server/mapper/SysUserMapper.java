package com.atp.server.mapper;

import com.atp.pojo.entity.sys.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysUserMapper {

    SysUser selectByUsername(@Param("username") String username);

    int updateLastLoginAt(@Param("id") Long id);
}
