package cn.wyq.serverwebsocket.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageVO {
    private String role;    // user 或 ai
    private String content; // 聊天内容
    private String mood;    // 情绪标签（如果截获到了的话）
}