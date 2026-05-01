package cn.wyq.serverwebsocket.enumeration;

import lombok.Getter;

/**
 * WebSocket 消息类型枚举
 */
@Getter
public enum WsMessageTypeEnum {

    CHAT(1, "普通群聊消息"),
    SYSTEM_NOTICE(2, "系统通知(如用户进退群)"),
    RED_PACKET_GRABBED(3, "红包被抢动态通知");

    private final Integer code;
    private final String desc;

    WsMessageTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}