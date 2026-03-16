package cn.wyq.serverwebsocket.pojo.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 消息表实体类
 * 对应表名：conversation_message
 */
@Data
public class ConversationMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 消息id
     */
    private Integer id;

    /**
     * 消息所属对话id
     */
    private Integer conversationId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息所属角色（1：用户，2：系统）
     */
    private Integer role;
}