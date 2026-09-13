package cn.wyq.serverwebsocket.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 登录审计日志实体。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginAuditLog {
    private Long id;
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
    private LocalDateTime createdAt;
}
