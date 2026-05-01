package cn.wyq.serverwebsocket.pojo.socket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 广播事件载体
 * 放在所有模块都能访问到的底层（如 pojo 模块）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WsBroadcastEvent {

    /**
     * 要发送的群聊或房间ID
     */
    private String groupId;

    /**
     * 组装好的 JSON 字符串消息
     */
    private String jsonMessage;
}