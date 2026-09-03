package cn.wyq.serverwebsocket.pojo.socket;

import lombok.Data;

/**
 * @version v1.0
 * @ClassName: ResultMessage
 * @Description: 用来封装服务端给浏览器发送的消息数据
 * @Author: wyq
 */
/**
 * ResultMessage：WebSocket 通信对象，负责封装实时连接或消息数据。
 */
@Data
public class ResultMessage {
    private boolean isSystem;
    private String fromName;
    private Object message;//如果是系统消息是数组
}
