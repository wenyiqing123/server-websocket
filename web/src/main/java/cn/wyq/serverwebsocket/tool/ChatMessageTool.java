package cn.wyq.serverwebsocket.tool;

import cn.wyq.serverwebsocket.service.FriendService;
import cn.wyq.serverwebsocket.service.IUserServiceTool;
import cn.wyq.serverwebsocket.service.SysConfigService;
import cn.wyq.serverwebsocket.socket.WebSocket;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * ChatMessageTool：AI/业务工具组件，为模型或业务流程提供可调用能力。
 */
@Component
@RequiredArgsConstructor
public class ChatMessageTool {

    private static final String CURRENT_USER_NAME = "currentUserName";

    private final IUserServiceTool userServiceTool;
    private final FriendService friendService;
    private final SysConfigService sysConfigService;

    @Tool(description = "以当前登录用户本人的身份，给其好友发送一条私聊消息。仅当用户明确要求发送消息时调用；recipientName 必须是准确用户名，content 必须是最终要发送的正文。")
    public String sendPrivateMessage(
            @ToolParam(description = "接收消息的准确用户名") String recipientName,
            @ToolParam(description = "最终发送给对方的消息正文，不要附加解释") String content,
            ToolContext toolContext) {
        try {
            String enabled = sysConfigService.getConfigMap()
                    .getOrDefault("ai_service_message_tool_enabled", "true");
            if (!("true".equalsIgnoreCase(enabled) || "1".equals(enabled))) {
                return "发送失败：管理员当前已关闭客服代发消息功能。";
            }
        } catch (RuntimeException exception) {
            return "发送失败：暂时无法确认代发消息功能状态。";
        }
        Object currentUserValue = toolContext.getContext().get(CURRENT_USER_NAME);
        String currentUserName = currentUserValue == null ? null : currentUserValue.toString();
        String targetName = recipientName == null ? null : recipientName.trim();
        String messageContent = content == null ? null : content.trim();

        if (!StringUtils.hasText(currentUserName)) {
            return "发送失败：无法识别当前登录用户。";
        }
        if (!StringUtils.hasText(targetName) || !StringUtils.hasText(messageContent)) {
            return "发送失败：接收人和消息正文不能为空。";
        }
        if (currentUserName.equals(targetName)) {
            return "发送失败：不能给自己发送代发消息。";
        }
        if (messageContent.length() > 1000) {
            return "发送失败：消息正文不能超过1000字。";
        }
        boolean targetExists = userServiceTool.lambdaQuery()
                .eq(cn.wyq.serverwebsocket.pojo.entity.User::getUserName, targetName)
                .count() > 0;
        if (!targetExists) {
            return "发送失败：没有找到用户名为“" + targetName + "”的用户，请先确认准确用户名。";
        }
        java.util.List<String> friendNames = friendService.getMyFriendsNameByUsername(currentUserName);
        if (friendNames == null || !friendNames.contains(targetName)) {
            return "发送失败：“" + targetName + "”不是当前用户的好友，不能直接发送私聊消息。";
        }

        WebSocket.sendChatMessage(currentUserName, targetName, messageContent);
        return "发送成功：已以当前用户身份向“" + targetName + "”发送消息。";
    }
}
