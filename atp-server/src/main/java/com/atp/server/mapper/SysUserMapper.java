package com.atp.server.mapper;

import com.atp.pojo.entity.sys.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统用户访问。工单模块要用它取姓名和角色。
 */
@Mapper
public interface SysUserMapper {

    /**
     * 按用户名查，登录用
     * @param username
     * @return
     */
    SysUser selectByUsername(@Param("username") String username);

    /**
     * 按 ID 查，工单列表显示姓名用
     * @param id
     * @return
     */
    SysUser getById(@Param("id") Long id);

    /** 当前用户有哪些角色编码，用来判断数据可见范围 */
    List<String> listRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * 更新最后登录时间
     * @param id
     * @return
     */
    int updateLastLoginAt(@Param("id") Long id);
}
