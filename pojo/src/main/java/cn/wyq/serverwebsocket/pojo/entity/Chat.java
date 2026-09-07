package cn.wyq.serverwebsocket.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Chat：业务实体对象，用于映射和传递持久化数据。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Chat {

    private String id;
    /** 不可变的会话所属用户编号，用于授权校验，避免用户名变更或复用造成越权。 */
    private Integer ownerUserId;
    private String userId;
    private String title;
    /** 自定义智能体 ID，旧 girlfriend/service/document 会话为空。 */
    private Long agentId;
    /** 创建会话时使用的提示词版本。 */
    private Integer promptVersion;
    private LocalDate createdAt; // 对应数据库的 created_at

    public Chat(String id, String userId, String title, LocalDate createdAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.createdAt = createdAt;
    }
}
