package cn.wyq.serverwebsocket.pojo.dto;

import lombok.Data;

/**
 * GrabRedPacketDTO：数据传输对象，用于承载接口请求参数。
 */
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