package com.atp.server.mapper;

import com.atp.pojo.entity.sys.SysUser;
import com.atp.pojo.vo.sys.UserVO;
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

    /**
     * 当前用户有哪些角色编码，用来判断数据可见范围
     * @param userId
     * @return
     */
    List<String> listRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * 更新最后登录时间
     * @param id
     * @return
     */
    int updateLastLoginAt(@Param("id") Long id);

    /**
     * 按关键词或角色查用户，两个条件都可空
     * @param keyword 关键词，匹配姓名或用户名
     * @param roleCode 角色编码，如 agent
     * @return 用户列表
     */
    List<UserVO> listUsers(@Param("keyword") String keyword, @Param("roleCode") String roleCode);

    /**
     * 查询可以被指派工单的内部人员。
     *
     * <p>只有客服、主管和管理员能作为处理人；提交人只能提交工单，
     * 不能作为工单处理人。</p>
     *
     * @return 可指派用户列表
     */
    List<UserVO> listAssignableUsers();

    /**
     * 查一个人的姓名，转派工单时要用来回显 "已转给张三"
     * @param id 用户 ID
     * @return 姓名，取不到返回 null
     */
    String getNameById(@Param("id") Long id);
}

