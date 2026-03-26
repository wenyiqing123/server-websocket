package cn.wyq.serverwebsocket.tool;

import cn.wyq.serverwebsocket.pojo.entity.User;
import cn.wyq.serverwebsocket.service.IUserServiceTool;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserTool {
    private final IUserServiceTool userServiceTool;

    // 💡 进化 1：极其详尽的描述。这是给大模型看的“说明书”，越详细，大模型调用越准！
    @Tool(description = "精确检索用户档案。当被问到‘系统中叫xx的人存在吗’、‘有哪些管理员’时调用。注意：为保护隐私，此接口最多只返回前20名匹配的用户。")
    public List<User> queryUser(@ToolParam(description = "用户查询条件对象，可包含userName、role等", required = false) User user) {

        // 💡 进化 2：防爆盾。如果条件为空，绝对不能查全表，可以默认返回最新的几个活跃用户，或者直接限制条数
        if (user == null) {
            return userServiceTool.lambdaQuery()
                    .select(User::getId, User::getUserName, User::getRole) // 💡 进化 3：过滤网。只给AI看必要的字段，绝对不要把 password、phone 等传给它！
                    .orderByDesc(User::getId)
                    .last("LIMIT 20") // 💡 防爆盾：强制兜底，绝不撑爆上下文
                    .list();
        }

        return userServiceTool.lambdaQuery()
                // 过滤网：只查询安全字段
                .select(User::getId, User::getUserName, User::getRole)
                .eq(user.getId() != null, User::getId, user.getId())
                .like(StringUtils.hasText(user.getUserName()), User::getUserName, user.getUserName())
                .eq(user.getRole() != null, User::getRole, user.getRole())
                // 防爆盾：加上 LIMIT 控制
                .last("LIMIT 20")
                .list();
    }
}