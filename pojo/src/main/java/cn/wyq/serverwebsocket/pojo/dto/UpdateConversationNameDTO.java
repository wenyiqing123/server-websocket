package cn.wyq.serverwebsocket.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateConversationNameDTO：数据传输对象，用于承载接口请求参数。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor

public class UpdateConversationNameDTO {
    private int conversationId;
    private String name;
}
