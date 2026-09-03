package cn.wyq.serverwebsocket.pojo.socket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * MessageInfo：WebSocket 通信对象，负责封装实时连接或消息数据。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageInfo {
    private Integer id;
    private String message;
    private String fromName;
    private String toName;
    private LocalDateTime time;
//    private int recalled;

}
