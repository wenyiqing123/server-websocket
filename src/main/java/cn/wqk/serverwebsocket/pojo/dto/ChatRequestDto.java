package cn.wqk.serverwebsocket.pojo.dto;

import lombok.Data;

@Data
public class ChatRequestDto {
    /**
     * 对话ID (如果是新对话，前端可能传null，但在你的Controller中/new是分开的，
     * 所以这里通常必须传有效的ID)
     */
    private Integer conversationId;

    /**
     * 用户发送的内容
     */
    private String content;

    /**
     * 模型名称 (可选，预留给未来切换 deepseek-v3 / r1 等)
     */
    private String model;
}