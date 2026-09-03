package cn.wyq.serverwebsocket.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 统一的 WebSocket 通信消息体
 */
/**
 * WsMessage：数据传输对象，用于承载接口请求参数。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WsMessage {

    /**
     * 消息类型 (对应 WsMessageTypeEnum 的 code)
     * 前端 Vue 第一时间就是通过判断这个值，来决定渲染什么 UI 组件
     */
    private Integer messageType;

    /**
     * 目标房间/群聊 ID
     */
    private String groupId;

    /**
     * 发送者 ID (如果是系统发出的，可以为 0 或 null)
     */
    private Integer senderId;

    /**
     * 供前端直接展示的纯文本内容
     * 例如："用户 张三 抢到了 12.50 元红包"
     */
    private String content;

    /**
     * 🌟 核心扩展字段：供前端程序底层调用的业务对象数据 (JSON格式)
     * 因为用的是 Object 类型，你可以往里面塞 Map，塞 Entity，或者塞自定义的内部类
     */
    private Object data;

    /**
     * 消息产生的时间
     */
    private LocalDateTime timestamp;
}