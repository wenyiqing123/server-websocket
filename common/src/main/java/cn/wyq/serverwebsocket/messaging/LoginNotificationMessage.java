package cn.wyq.serverwebsocket.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 登录后的异步站内/WebSocket 通知消息。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginNotificationMessage {
    private String eventId;
    private Integer userId;
    /** 接收通知的好友用户名。 */
    private String username;
    /** 登录成功、刚刚上线的用户用户名。 */
    private String sourceUsername;
    /** 前端展示用的通知分类。 */
    private String notificationType;
    private String content;
    private String ipAddress;
    private String userAgent;
}
