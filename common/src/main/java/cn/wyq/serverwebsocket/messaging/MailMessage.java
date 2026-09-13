package cn.wyq.serverwebsocket.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 登录模块异步邮件消息。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailMessage {
    private String eventId;
    private String toEmail;
    private String subject;
    private String content;
    private String templateType;
}
