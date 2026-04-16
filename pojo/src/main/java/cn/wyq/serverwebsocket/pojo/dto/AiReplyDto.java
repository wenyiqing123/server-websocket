package cn.wyq.serverwebsocket.pojo.dto;
import lombok.Data;

@Data
public class AiReplyDto {
    private String context;      // 最近的聊天记录拼接字符串
    private String myUsername;   // 发送人
    private String targetName;   // 接收人/群名
}