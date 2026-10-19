package com.atp.pojo.vo.sys;

import lombok.Data;

import java.util.List;

/**
 * 用户出参。roleNames 是关联 sys_role 查出来的，user_query 工具要它。
 */
@Data
public class UserVO {

    //用户 ID
    private Long id;

    //用户名
    private String username;

    //姓名
    private String name;

    //角色名称列表，只做展示；listUsers 那条 SQL 不填它，由调用方补
    private List<String> roleNames;
}
