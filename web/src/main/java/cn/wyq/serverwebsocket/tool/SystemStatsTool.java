package cn.wyq.serverwebsocket.tool;

import cn.wyq.serverwebsocket.pojo.entity.Message;
import cn.wyq.serverwebsocket.pojo.entity.User;
import cn.wyq.serverwebsocket.service.IMessageServiceTool;
import cn.wyq.serverwebsocket.service.IUserServiceTool;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SystemStatsTool {

    private final IUserServiceTool userServiceTool;
    private final IMessageServiceTool messageServiceTool;

    @Tool(description = "获取系统的全局数据看板。当用户问‘系统一共有多少人’、‘消息总数是多少’、‘管理员有几个’这种宏观统计问题时，务必调用此工具。")
    public Map<String, Object> getSystemDashboard() {
        Map<String, Object> stats = new HashMap<>();

        // 1. 用户维度统计
        long totalUsers = userServiceTool.count();
        long superAdmins = userServiceTool.count(new LambdaQueryWrapper<User>().eq(User::getRole, 0));
        long normalAdmins = userServiceTool.count(new LambdaQueryWrapper<User>().eq(User::getRole, 1));
        long normalUsers = userServiceTool.count(new LambdaQueryWrapper<User>().eq(User::getRole, 2));

        // 2. 消息维度统计
        long totalMessages = messageServiceTool.count();

        // 💡 架构升级：统计撤回消息（message 为 null 或者 为空字符串）
        long recalledMessages = messageServiceTool.count(new LambdaQueryWrapper<Message>()
                .and(w -> w.isNull(Message::getMessage).or().eq(Message::getMessage, "")));

        // 💡 架构升级：新增统计群发消息（to_name 为 null 或者 为空字符串）
        long groupMessages = messageServiceTool.count(new LambdaQueryWrapper<Message>()
                .and(w -> w.isNull(Message::getToName).or().eq(Message::getToName, "")));

        stats.put("totalUsers", totalUsers);
        stats.put("superAdmins", superAdmins);
        stats.put("normalAdmins", normalAdmins);
        stats.put("normalUsers", normalUsers);

        stats.put("totalMessages", totalMessages);
        stats.put("validMessages", totalMessages - recalledMessages); // 有效单聊+群发消息
        stats.put("recalledMessages", recalledMessages);              // 撤回消息数
        stats.put("groupMessages", groupMessages);                    // 群发消息数

        return stats;
    }
}