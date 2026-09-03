package cn.wyq.serverwebsocket.pojo.socket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * MessageFull：WebSocket 通信对象，负责封装实时连接或消息数据。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageFull {
    private Integer id;
    private String message;
    private String fromName;
    private String fromPath;
    private String toName;
    private String toPath;
    private LocalDateTime time;
//    private int recalled;
}
