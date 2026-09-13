package cn.wyq.serverwebsocket.pojo.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理员修改系统内置智能体的请求参数。
 *
 * <p>内置智能体不属于某个用户，因此单独使用 DTO，避免与用户自定义智能体的
 * 所有权和删除字段混用。</p>
 */
@Data
public class AiBuiltinAgentUpdateDTO {
    /** 展示名称。 */
    private String name;
    /** 头像地址。 */
    private String avatarUrl;
    /** 开启会话时展示的欢迎语。 */
    private String welcomeMessage;
    /** 系统人设提示词。 */
    private String systemPrompt;
    /** 使用的模型名称。 */
    private String model;
    /** 模型温度。 */
    private BigDecimal temperature;
    /** 最大输出 token 数。 */
    private Integer maxTokens;
    /** 专属主题色。 */
    private String themeColor;
    /** 是否启用：0 禁用，1 启用。 */
    private Integer status;
}
