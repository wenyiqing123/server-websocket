package cn.wyq.serverwebsocket.pojo.dto;
import lombok.Data;

import java.util.List;

/**
 * AiReplyDto：数据传输对象，用于承载接口请求参数。
 */
@Data
public class AiReplyDto {
    private List<MessageItem> messages;
    private String targetName;
    private String conversationType;
    private String draft;

    /**
     * MessageItem：数据传输对象，用于承载接口请求参数。
     */
    @Data
    public static class MessageItem {
        private String senderName;
        private String content;
    }
}
