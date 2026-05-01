package cn.wyq.serverwebsocket.pojo.dto;

import lombok.Data;

@Data
public class GrabRedPacketDTO {
    /**
     * 要抢的红包主键ID
     */
    private Long redPacketId;

    /**
     * 所在的群聊/房间ID (用于广播)
     */
    private String groupId;
}