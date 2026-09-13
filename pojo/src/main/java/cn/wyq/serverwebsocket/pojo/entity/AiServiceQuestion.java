package cn.wyq.serverwebsocket.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 智能客服问题题库实体。
 *
 * <p>题库按归一化问题的 SHA-256 哈希去重，同时记录使用次数，
 * 用于启动预热和输入联想排序。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiServiceQuestion {

    /** 数据库主键。 */
    private Long id;
    /** 归一化问题的 SHA-256 哈希。 */
    private String questionHash;
    /** 用户首次提交的原始问题。 */
    private String question;
    /** 去除空白和标点后的匹配文本。 */
    private String normalizedQuestion;
    /** 题库来源，当前客服问题固定为 service。 */
    private String sourceType;
    /** 问题被发送的次数。 */
    private Integer useCount;
    /** 最近一次提交该问题的用户编号。 */
    private Integer lastUserId;
    /** 最近一次使用时间。 */
    private LocalDateTime lastUsedAt;
    /** 首次创建时间。 */
    private LocalDateTime createdAt;
    /** 最近更新时间。 */
    private LocalDateTime updatedAt;
    /** 是否允许在用户输入联想中展示。 */
    private Integer enabled;
    /** 审核状态：PENDING、APPROVED 或 REJECTED。 */
    private String reviewStatus;
    /** 管理员维护备注。 */
    private String adminRemark;
}
