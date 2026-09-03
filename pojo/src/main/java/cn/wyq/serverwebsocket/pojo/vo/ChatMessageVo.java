package cn.wyq.serverwebsocket.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ChatMessageVo：视图对象，用于组织接口返回和展示数据。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageVo {
    private String role;    // user 或 ai
    private String content; // 聊天内容
    private String mood;    // 情绪标签（如果截获到了的话）
}