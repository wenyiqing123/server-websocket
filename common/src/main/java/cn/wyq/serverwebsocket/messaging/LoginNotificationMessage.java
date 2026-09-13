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
    private String username;
    private String content;
    private String ipAddress;
    private String userAgent;
}
