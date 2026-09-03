package cn.wyq.serverwebsocket.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ConversationMessageDTO：数据传输对象，用于承载接口请求参数。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor

public class ConversationMessageDTO {
    private int conversationId;
    private String content;
    private int role;
}
