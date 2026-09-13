package cn.wyq.serverwebsocket.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 登录、注册和验证码请求的异步审计消息。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginAuditMessage {
    private String eventId;
    private Integer userId;
    private String username;
    private String eventType;
    private Boolean success;
    private Boolean remoteLogin;
    private String previousIp;
    private String ipAddress;
    private String userAgent;
    private String failureReason;
    private LocalDateTime occurredAt;
}
