package com.atp.server.agent.tool.impl;

import com.atp.agent.tool.AbstractAgentTool;
import com.atp.agent.tool.ToolContext;
import com.atp.agent.tool.ToolResult;
import com.atp.pojo.vo.sys.UserVO;
import com.atp.server.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 查用户。
 * 转派工单前模型要能把“张三”翻译成 userId。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserQueryTool extends AbstractAgentTool {

    private static final int MAX_ROWS = 20;

    private final SysUserMapper sysUserMapper;

    /**
     * 工具编码，必须与 agent_tool.code 一致。
     *
     * @return 工具编码
     */
    @Override
    public String code() {
        return "user_query";
    }

    /**
     * 声明工具参数类型。
     *
     * @return 参数名到参数类型的映射
     */
    @Override
    public Map<String, Class<?>> paramTypes() {
        return Map.of(
                "keyword", String.class,
                "role", String.class
        );
    }

    /**
     * 按关键词或角色查询用户。
     *
     * @param args 工具参数
     * @param ctx 执行上下文
     * @return 用户列表及给模型转述的文本
     */
    @Override
    public ToolResult execute(Map<String, Object> args, ToolContext ctx) {
        String keyword = str(args, "keyword");
        String role = str(args, "role");
        try {
            List<UserVO> users = sysUserMapper.listUsers(keyword, role);
            if (users == null || users.isEmpty()) {
                return ToolResult.ok(List.of(), "没有匹配到用户。");
            }
            if (users.size() > MAX_ROWS) {
                users = new ArrayList<>(users.subList(0, MAX_ROWS));
            }

            List<Map<String, Object>> data = new ArrayList<>();
            StringBuilder sb = new StringBuilder("匹配到 ").append(users.size()).append(" 个用户：\n");
            for (UserVO user : users) {
                List<String> roles = sysUserMapper.listRoleCodesByUserId(user.getId());
                user.setRoleNames(roles);
                data.add(new LinkedHashMap<>(Map.of(
                        "userId", user.getId(),
                        "username", user.getUsername(),
                        "name", user.getName() == null ? "" : user.getName(),
                        "roles", roles)));
                sb.append("- ").append(user.getName()).append("（").append(user.getUsername())
                        .append("，角色：").append(String.join("/", roles)).append("）\n");
            }
            sb.append("如果要指派工单，请让用户确认是哪一个，不要自己挑。");
            return ToolResult.ok(data, sb.toString());
        } catch (Exception e) {
            log.error("工具 user_query 执行失败", e);
            return ToolResult.fail("查询用户失败：" + e.getMessage());
        }
    }
}