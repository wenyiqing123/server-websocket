package cn.wyq.serverwebsocket.pojo.dto;
import lombok.Data;

import java.util.List;

@Data
public class AiReplyDto {
    private List<MessageItem> messages;
    private String targetName;
    private String conversationType;
    private String draft;

    @Data
    public static class MessageItem {
        private String senderName;
        private String content;
    }
}
