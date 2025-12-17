package cn.wyq.serverwebsocket.socket.pojo;

import lombok.Data;

/**
 * @version v1.0
 * @ClassName: ResultMessage
 * @Description: 用来封装服务端给浏览器发送的消息数据
 * @Author: wyq
 */
@Data
public class ResultMessage {
    private boolean isSystem;
    private String fromName;
    private Object message;//如果是系统消息是数组
}
