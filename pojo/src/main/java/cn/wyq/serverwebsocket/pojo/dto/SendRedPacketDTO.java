package cn.wyq.serverwebsocket.pojo.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SendRedPacketDTO {
    /**
     * 群聊/房间ID
     */
    private String groupId;
    private Integer userId;

    /**
     * 红包总金额
     */
    private BigDecimal totalAmount;

    /**
     * 红包总个数
     */
    private Integer totalCount;
    private String receiverName;
}