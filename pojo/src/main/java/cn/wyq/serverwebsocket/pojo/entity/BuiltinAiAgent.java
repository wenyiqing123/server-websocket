package cn.wyq.serverwebsocket.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 系统内置智能体的运行配置。
 *
 * <p>目前配置存储在 {@code sys_config} 中，使用该对象向前端提供统一的智能体
 * 结构，后续迁移到 {@code ai_agent} 表时无需修改页面协议。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuiltinAiAgent {
    /** 固定标识，目前为 girlfriend。 */
    private String key;
    /** 内置智能体名称。 */
    private String name;
    /** 头像地址。 */
    private String avatarUrl;
    /** 欢迎语。 */
    private String welcomeMessage;
    /** 系统提示词。 */
    private String systemPrompt;
    /** 模型名称。 */
    private String model;
    /** 温度参数。 */
    private BigDecimal temperature;
    /** 最大输出 token 数。 */
    private Integer maxTokens;
    /** 专属主题色。 */
    private String themeColor;
    /** 是否启用：0 禁用，1 启用。 */
    private Integer status;
    /** 标记为系统内置对象。 */
    private boolean builtin;
}
