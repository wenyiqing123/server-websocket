package cn.wyq.serverwebsocket.pojo.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 创建自定义智能体请求。 */
@Data
public class AiAgentCreateDTO {
    private String name;
    private String avatarUrl;
    private String systemPrompt;
    private String welcomeMessage;
    private String model;
    private BigDecimal temperature;
    private Integer maxTokens;
    private String visibility;
}
