package cn.wyq.serverwebsocket.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户自定义 AI 智能体实体。
 *
 * <p>智能体归属于 ownerUserId 对应的用户，deleted 仅表示软删除，不直接物理删除记录。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAgent {
    private Long id;
    private Integer ownerUserId;
    private String ownerUserName;
    private String name;
    private String avatarUrl;
    private String systemPrompt;
    private String welcomeMessage;
    private String model;
    private BigDecimal temperature;
    private Integer maxTokens;
    private String visibility;
    private Integer status;
    private Integer isBuiltin;
    private Integer promptVersion;
    private Integer deleted;
    private Long useCount;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
